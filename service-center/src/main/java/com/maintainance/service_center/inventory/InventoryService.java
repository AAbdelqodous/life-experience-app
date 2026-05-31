package com.maintainance.service_center.inventory;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.staff.CenterMembership;
import com.maintainance.service_center.staff.CenterMembershipRepository;
import com.maintainance.service_center.staff.CenterPermission;
import com.maintainance.service_center.staff.CenterSecurityService;
import com.maintainance.service_center.staff.MembershipStatus;
import com.maintainance.service_center.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Spec 025 — parts catalog, stock movements (RECEIVE/ADJUST/CONSUME/reversal), low-stock, and the
 * inventory report. On-hand is a cached running total kept in step with the movement ledger (R2);
 * consumption decrements atomically so concurrent technicians can't double-spend (R3, SC-002).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final PartRepository partRepository;
    private final StockMovementRepository movementRepository;
    private final MaintenanceCenterRepository centerRepository;
    private final CenterMembershipRepository membershipRepository;
    private final CenterSecurityService centerSecurity;

    // ── Catalog CRUD ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PartResponse> listParts(User caller, String search, boolean lowStock) {
        MaintenanceCenter center = resolveMyCenter(caller);
        // Either permission may view the catalog: MANAGE_INVENTORY (manage) or CONSUME_PARTS (picker).
        centerSecurity.requirePermission(center.getId(), caller, CenterPermission.CONSUME_PARTS);
        List<Part> parts;
        if (lowStock) {
            parts = partRepository.findLowStock(center.getId());
        } else if (search != null && !search.isBlank()) {
            parts = partRepository.search(center.getId(), search.trim());
        } else {
            parts = partRepository.findByCenterIdOrderByNameEnAsc(center.getId());
        }
        return parts.stream().map(this::toPartResponse).toList();
    }

    @Transactional
    public PartResponse createPart(User caller, CreatePartRequest req) {
        MaintenanceCenter center = resolveMyCenter(caller);
        centerSecurity.requirePermission(center.getId(), caller, CenterPermission.MANAGE_INVENTORY);
        validatePrices(req.costPrice(), req.salePrice());
        if (partRepository.existsByCenterIdAndSku(center.getId(), req.sku())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A part with this SKU already exists");
        }
        Part part = Part.builder()
                .center(center).nameAr(req.nameAr()).nameEn(req.nameEn()).sku(req.sku())
                .category(req.category()).unit(req.unit()).costPrice(req.costPrice())
                .salePrice(req.salePrice()).supplier(req.supplier())
                .reorderThreshold(req.reorderThreshold()).onHand(0).isActive(true)
                .build();
        partRepository.save(part);
        log.info("Created part {} (sku {}) for center {}", part.getId(), part.getSku(), center.getId());
        return toPartResponse(part);
    }

    @Transactional
    public PartResponse updatePart(User caller, Long partId, UpdatePartRequest req) {
        MaintenanceCenter center = resolveMyCenter(caller);
        centerSecurity.requirePermission(center.getId(), caller, CenterPermission.MANAGE_INVENTORY);
        validatePrices(req.costPrice(), req.salePrice());
        Part part = requirePart(partId, center.getId());
        if (!part.getSku().equals(req.sku())
                && partRepository.existsByCenterIdAndSku(center.getId(), req.sku())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A part with this SKU already exists");
        }
        part.setNameAr(req.nameAr());
        part.setNameEn(req.nameEn());
        part.setSku(req.sku());
        part.setCategory(req.category());
        part.setUnit(req.unit());
        part.setCostPrice(req.costPrice());
        part.setSalePrice(req.salePrice());
        part.setSupplier(req.supplier());
        part.setReorderThreshold(req.reorderThreshold());
        if (req.isActive() != null) {
            part.setActive(req.isActive());
        }
        partRepository.save(part);
        return toPartResponse(part);
    }

    @Transactional
    public void deactivatePart(User caller, Long partId) {
        MaintenanceCenter center = resolveMyCenter(caller);
        centerSecurity.requirePermission(center.getId(), caller, CenterPermission.MANAGE_INVENTORY);
        Part part = requirePart(partId, center.getId());
        part.setActive(false); // soft delete — kept for history; excluded from new quotes
        partRepository.save(part);
    }

    // ── Stock movements ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MovementResponse> getMovements(User caller, Long partId) {
        MaintenanceCenter center = resolveMyCenter(caller);
        centerSecurity.requirePermission(center.getId(), caller, CenterPermission.MANAGE_INVENTORY);
        requirePart(partId, center.getId());
        return movementRepository.findByPartIdOrderByCreatedAtDesc(partId).stream()
                .map(this::toMovementResponse).toList();
    }

    @Transactional
    public PartResponse receiveStock(User caller, Long partId, ReceiveStockRequest req) {
        MaintenanceCenter center = resolveMyCenter(caller);
        centerSecurity.requirePermission(center.getId(), caller, CenterPermission.MANAGE_INVENTORY);
        Part part = requirePart(partId, center.getId());
        part.setOnHand(part.getOnHand() + req.quantity());
        partRepository.save(part);
        saveMovement(part, MovementType.RECEIVE, req.quantity(), req.unitCost(), null, null, caller);
        return toPartResponse(part);
    }

    @Transactional
    public PartResponse adjustStock(User caller, Long partId, AdjustStockRequest req) {
        MaintenanceCenter center = resolveMyCenter(caller);
        centerSecurity.requirePermission(center.getId(), caller, CenterPermission.MANAGE_INVENTORY);
        Part part = requirePart(partId, center.getId());
        int delta = req.newOnHand() - part.getOnHand();
        part.setOnHand(req.newOnHand());
        partRepository.save(part);
        saveMovement(part, MovementType.ADJUST, delta, null, req.reason(), null, caller);
        return toPartResponse(part);
    }

    // ── Consumption (called from the quote flow — not a public endpoint) ──────────

    /** Atomically decrement stock for a catalogued part used on a booking's quote (R3). */
    @Transactional
    public void consume(Long centerId, Long partId, int quantity, Booking booking, User actor) {
        if (quantity <= 0) {
            return;
        }
        Part part = partRepository.findByIdAndCenterId(partId, centerId).orElse(null);
        if (part == null) {
            return; // unknown/foreign part (e.g. ad-hoc) — no stock effect
        }
        partRepository.decrementOnHand(partId, quantity); // atomic; may go negative (backorder, visible)
        saveMovement(part, MovementType.CONSUME, -quantity, null, null, booking, actor);
        log.info("Consumed {} of part {} for booking {}", quantity, partId,
                booking != null ? booking.getId() : null);
    }

    /**
     * Returns the booking's <em>outstanding</em> consumed stock (Σ CONSUME + Σ CONSUME_REVERSAL per
     * part) — used when a quote is superseded or the booking is cancelled (R5). Net-based, so it is
     * idempotent and correct across multiple quote revisions (a second call finds nothing to undo).
     */
    @Transactional
    public void reverseConsumption(Long bookingId, User actor) {
        Map<Long, Integer> net = new LinkedHashMap<>();
        Map<Long, Part> parts = new LinkedHashMap<>();
        Booking booking = null;
        for (StockMovement m : movementRepository.findByBookingId(bookingId)) {
            if (m.getType() == MovementType.CONSUME || m.getType() == MovementType.CONSUME_REVERSAL) {
                net.merge(m.getPart().getId(), m.getQuantity(), Integer::sum);
                parts.putIfAbsent(m.getPart().getId(), m.getPart());
                if (booking == null) {
                    booking = m.getBooking(); // keep reversals linked to the booking (idempotency)
                }
            }
        }
        int reversed = 0;
        for (Map.Entry<Long, Integer> e : net.entrySet()) {
            int outstanding = -e.getValue(); // net is negative while still consumed
            if (outstanding <= 0) {
                continue;
            }
            partRepository.incrementOnHand(e.getKey(), outstanding);
            saveMovement(parts.get(e.getKey()), MovementType.CONSUME_REVERSAL, outstanding,
                    null, null, booking, actor);
            reversed++;
        }
        if (reversed > 0) {
            log.info("Reversed outstanding consumption for {} part(s) on booking {}", reversed, bookingId);
        }
    }

    // ── Low-stock + report ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<LowStockItem> getLowStock(User caller) {
        MaintenanceCenter center = resolveMyCenter(caller);
        centerSecurity.requirePermission(center.getId(), caller, CenterPermission.MANAGE_INVENTORY);
        return partRepository.findLowStock(center.getId()).stream().map(this::toLowStock).toList();
    }

    @Transactional(readOnly = true)
    public InventoryReportResponse getReport(User caller, LocalDate from, LocalDate to) {
        MaintenanceCenter center = resolveMyCenter(caller);
        centerSecurity.requirePermission(center.getId(), caller, CenterPermission.VIEW_REPORTS);
        List<Part> parts = partRepository.findByCenterIdOrderByNameEnAsc(center.getId());

        BigDecimal stockValue = parts.stream()
                .filter(p -> p.getOnHand() > 0)
                .map(p -> p.getCostPrice().multiply(BigDecimal.valueOf(p.getOnHand())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, Part> byId = new LinkedHashMap<>();
        parts.forEach(p -> byId.put(p.getId(), p));

        // Net consumed per part over the range (CONSUME negative, reversal positive).
        Map<Long, Integer> consumed = new LinkedHashMap<>();
        List<StockMovement> moves = movementRepository.findByPart_Center_IdAndCreatedAtBetween(
                center.getId(), from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        for (StockMovement m : moves) {
            if (m.getType() == MovementType.CONSUME || m.getType() == MovementType.CONSUME_REVERSAL) {
                consumed.merge(m.getPart().getId(), -m.getQuantity(), Integer::sum);
            }
        }

        List<UsageRow> usage = consumed.entrySet().stream()
                .filter(e -> e.getValue() != 0 && byId.containsKey(e.getKey()))
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .map(e -> {
                    Part p = byId.get(e.getKey());
                    return new UsageRow(p.getId(), p.getNameEn(), p.getNameAr(), e.getValue());
                })
                .toList();

        BigDecimal partsMargin = usage.stream()
                .map(u -> {
                    Part p = byId.get(u.partId());
                    return p.getSalePrice().subtract(p.getCostPrice())
                            .multiply(BigDecimal.valueOf(u.consumedQty()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Long> fastMovers = usage.stream().limit(5).map(UsageRow::partId).toList();
        List<Long> slowMovers = usage.stream()
                .sorted(Comparator.comparingInt(UsageRow::consumedQty))
                .limit(5).map(UsageRow::partId).toList();

        List<LowStockItem> reorder = partRepository.findLowStock(center.getId()).stream()
                .map(this::toLowStock).toList();

        return new InventoryReportResponse(from, to, stockValue, usage, fastMovers, slowMovers,
                partsMargin, reorder);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    /** Resolves a part's current sale price for a center — used to snapshot onto quote lines (R4). */
    @Transactional(readOnly = true)
    public Part findActivePart(Long centerId, Long partId) {
        return partRepository.findByIdAndCenterId(partId, centerId)
                .filter(Part::isActive)
                .orElse(null);
    }

    private void saveMovement(Part part, MovementType type, int qty, BigDecimal unitCost,
                              String reason, Booking booking, User actor) {
        movementRepository.save(StockMovement.builder()
                .part(part).type(type).quantity(qty).unitCost(unitCost).reason(reason)
                .booking(booking).actor(actor).build());
    }

    private Part requirePart(Long partId, Long centerId) {
        return partRepository.findByIdAndCenterId(partId, centerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Part not found"));
    }

    private void validatePrices(BigDecimal cost, BigDecimal sale) {
        if (cost.signum() < 0 || sale.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prices must be non-negative");
        }
    }

    private MaintenanceCenter resolveMyCenter(User caller) {
        return centerRepository.findFirstByOwnerId(caller.getId())
                .or(() -> membershipRepository
                        .findByUserIdAndStatus(caller.getId(), MembershipStatus.ACTIVE)
                        .stream().findFirst().map(CenterMembership::getCenter))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "No center associated with this account"));
    }

    private PartResponse toPartResponse(Part p) {
        return new PartResponse(p.getId(), p.getNameAr(), p.getNameEn(), p.getSku(), p.getCategory(),
                p.getUnit(), p.getCostPrice(), p.getSalePrice(), p.getSupplier(),
                p.getReorderThreshold(), p.getOnHand(), p.isActive());
    }

    private MovementResponse toMovementResponse(StockMovement m) {
        return new MovementResponse(m.getId(), m.getPart().getId(), m.getType(), m.getQuantity(),
                m.getUnitCost(), m.getReason(),
                m.getBooking() != null ? m.getBooking().getId() : null,
                actorName(m.getActor()), m.getCreatedAt());
    }

    private LowStockItem toLowStock(Part p) {
        int suggested = Math.max(p.getReorderThreshold() * 2 - p.getOnHand(), 1);
        return new LowStockItem(p.getId(), p.getNameAr(), p.getNameEn(), p.getSku(),
                p.getOnHand(), p.getReorderThreshold(), p.getSupplier(), suggested);
    }

    private String actorName(User u) {
        if (u == null) {
            return "System";
        }
        String name = ((u.getFirstname() != null ? u.getFirstname() : "")
                + " " + (u.getLastname() != null ? u.getLastname() : "")).trim();
        return name.isEmpty() ? u.getEmail() : name;
    }
}
