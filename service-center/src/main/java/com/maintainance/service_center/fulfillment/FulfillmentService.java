package com.maintainance.service_center.fulfillment;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.booking.FulfillmentMode;
import com.maintainance.service_center.booking.ServiceAddress;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.staff.CenterMembership;
import com.maintainance.service_center.staff.CenterMembershipRepository;
import com.maintainance.service_center.staff.CenterPermission;
import com.maintainance.service_center.staff.CenterSecurityService;
import com.maintainance.service_center.staff.MembershipStatus;
import com.maintainance.service_center.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Spec 008 — fulfillment capability, fee computation, logistics, and saved addresses. A center may
 * author its own {@link FulfillmentCapability} (modes / service area / fees); when it hasn't, a
 * sensible platform default applies. The fee is snapshotted onto the booking at creation and appears
 * as an invoice line.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FulfillmentService {

    private static final List<String> DEFAULT_MODES =
            List.of("DROP_OFF", "PICKUP_DELIVERY", "AT_HOME");
    private static final List<String> DEFAULT_AREA =
            List.of("Hawalli", "Salmiya", "Capital", "Farwaniya");
    private static final BigDecimal PICKUP_BASE = new BigDecimal("3.000");
    private static final BigDecimal PICKUP_PER_KM = new BigDecimal("0.250");
    private static final BigDecimal AT_HOME_FLAT = new BigDecimal("5.000");

    /**
     * Ordered logistics legs the center drives, per mode. The first entry is the initial leg set at
     * creation ({@link #initialState}); the last is terminal. DROP_OFF has no legs (free, no logistics).
     */
    private static final List<String> PICKUP_LEGS = List.of(
            "PICKUP_SCHEDULED", "DRIVER_EN_ROUTE_PICKUP", "PICKED_UP",
            "AT_CENTER", "READY_FOR_RETURN", "OUT_FOR_DELIVERY", "DELIVERED");
    private static final List<String> AT_HOME_LEGS = List.of(
            "TECH_ASSIGNED", "TECH_EN_ROUTE", "TECH_ARRIVED", "SERVICE_IN_PROGRESS", "SERVICE_COMPLETED");

    private final SavedAddressRepository savedAddressRepository;
    private final BookingRepository bookingRepository;
    private final MaintenanceCenterRepository centerRepository;
    private final FulfillmentCapabilityRepository capabilityRepository;
    private final CenterMembershipRepository membershipRepository;
    private final CenterSecurityService centerSecurity;

    /** Effective fee rules for a center — its authored values or the platform default. */
    private record FeeParams(BigDecimal pickupBase, BigDecimal pickupPerKm, BigDecimal atHomeFlat) {}

    // ── Capability ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CapabilityResponse getCapability(Long centerId, Long serviceId) {
        MaintenanceCenter center = centerRepository.findById(centerId)
                .orElseThrow(() -> new EntityNotFoundException("Center not found: " + centerId));
        return buildCapability(center, serviceId);
    }

    /** Builds the capability view from the center's authored config, or the default when absent. */
    private CapabilityResponse buildCapability(MaintenanceCenter center, Long serviceId) {
        FulfillmentCapability cap = capabilityRepository.findByCenterId(center.getId()).orElse(null);
        List<String> modes = cap != null && !cap.getSupportedModes().isEmpty()
                ? cap.getSupportedModes() : DEFAULT_MODES;
        List<String> areas = cap != null && !cap.getServiceAreaGovernorates().isEmpty()
                ? cap.getServiceAreaGovernorates() : DEFAULT_AREA;
        FeeParams fees = feeParams(cap);

        Map<String, FeeRuleView> feeByMode = new LinkedHashMap<>();
        for (String mode : modes) {
            switch (mode) {
                case "DROP_OFF" -> feeByMode.put(mode, new FeeRuleView("FLAT", BigDecimal.ZERO, null, null));
                case "PICKUP_DELIVERY" -> feeByMode.put(mode,
                        new FeeRuleView("PER_KM", null, fees.pickupBase(), fees.pickupPerKm()));
                case "AT_HOME" -> feeByMode.put(mode, new FeeRuleView("FLAT", fees.atHomeFlat(), null, null));
                default -> { /* unknown mode — skip */ }
            }
        }
        return new CapabilityResponse(center.getId(), serviceId, modes, areas, feeByMode,
                center.getLatitude(), center.getLongitude());
    }

    /** Owner-side: read the center's authored capability (or the effective default). */
    @Transactional(readOnly = true)
    public CapabilityResponse getMyCapability(User caller) {
        MaintenanceCenter center = resolveMyCenter(caller);
        centerSecurity.requirePermission(center.getId(), caller, CenterPermission.MANAGE_PRICING);
        return buildCapability(center, null);
    }

    /** Owner-side: author the center's capability. DROP_OFF is always offered; fees validated per mode. */
    @Transactional
    public CapabilityResponse updateCapability(User caller, UpdateCapabilityRequest req) {
        MaintenanceCenter center = resolveMyCenter(caller);
        centerSecurity.requirePermission(center.getId(), caller, CenterPermission.MANAGE_PRICING);

        List<String> modes = new ArrayList<>();
        modes.add("DROP_OFF"); // always available — a customer can bring it to the center
        if (req.supportedModes() != null) {
            for (String m : req.supportedModes()) {
                if (("PICKUP_DELIVERY".equals(m) || "AT_HOME".equals(m)) && !modes.contains(m)) {
                    modes.add(m);
                }
            }
        }
        if (modes.contains("PICKUP_DELIVERY")
                && (req.pickupBase() == null || req.pickupBase().signum() < 0
                    || req.pickupPerKm() == null || req.pickupPerKm().signum() < 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Pickup base and per-km fees are required and must be ≥ 0 when pickup is offered");
        }
        if (modes.contains("AT_HOME") && (req.atHomeFlat() == null || req.atHomeFlat().signum() < 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "At-home flat fee is required and must be ≥ 0 when at-home is offered");
        }

        FulfillmentCapability cap = capabilityRepository.findByCenterId(center.getId())
                .orElseGet(() -> FulfillmentCapability.builder().centerId(center.getId()).build());
        cap.setSupportedModes(modes);
        cap.setServiceAreaGovernorates(req.serviceAreaGovernorates() != null
                ? new ArrayList<>(req.serviceAreaGovernorates()) : new ArrayList<>());
        cap.setPickupBase(req.pickupBase());
        cap.setPickupPerKm(req.pickupPerKm());
        cap.setAtHomeFlat(req.atHomeFlat());
        capabilityRepository.save(cap);
        log.info("Center {} fulfillment capability updated: modes={}", center.getId(), modes);
        return buildCapability(center, null);
    }

    private FeeParams feeParams(FulfillmentCapability cap) {
        return new FeeParams(
                cap != null && cap.getPickupBase() != null ? cap.getPickupBase() : PICKUP_BASE,
                cap != null && cap.getPickupPerKm() != null ? cap.getPickupPerKm() : PICKUP_PER_KM,
                cap != null && cap.getAtHomeFlat() != null ? cap.getAtHomeFlat() : AT_HOME_FLAT);
    }

    private FeeParams resolveFeeParams(Long centerId) {
        return feeParams(capabilityRepository.findByCenterId(centerId).orElse(null));
    }

    // ── Fee + address resolution (called from BookingService.create) ──────────────

    /** Result of preparing a booking's fulfillment: the resolved address, the fee, the first leg. */
    public record FulfillmentResult(ServiceAddress address, BigDecimal fee, String logisticsState) {}

    /**
     * Resolves the service address (saved or inline), computes the authoritative fee, and the initial
     * logistics state. DROP_OFF needs no address and is free; non-drop-off requires an address.
     */
    @Transactional(readOnly = true)
    public FulfillmentResult prepare(MaintenanceCenter center, User customer, FulfillmentMode mode,
                                     Long serviceAddressId, ServiceAddress inlineAddress) {
        FulfillmentMode resolved = mode != null ? mode : FulfillmentMode.DROP_OFF;
        if (resolved == FulfillmentMode.DROP_OFF) {
            return new FulfillmentResult(null, BigDecimal.ZERO, null);
        }
        ServiceAddress address = resolveAddress(customer, serviceAddressId, inlineAddress);
        if (address == null || address.getGovernorate() == null || address.getGovernorate().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A service address is required for pickup / at-home fulfillment");
        }
        BigDecimal fee = computeFee(resolved, address, center);
        return new FulfillmentResult(copy(address), fee, initialState(resolved));
    }

    private ServiceAddress resolveAddress(User customer, Long serviceAddressId, ServiceAddress inline) {
        if (serviceAddressId != null) {
            return savedAddressRepository.findByIdAndCustomerId(serviceAddressId, customer.getId())
                    .map(SavedAddress::getAddress)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saved address not found"));
        }
        return inline;
    }

    private BigDecimal computeFee(FulfillmentMode mode, ServiceAddress address, MaintenanceCenter center) {
        FeeParams fees = resolveFeeParams(center.getId());
        if (mode == FulfillmentMode.AT_HOME) {
            return fees.atHomeFlat();
        }
        // PICKUP_DELIVERY: base + per-km × round-trip-ish distance (one-way when no coords).
        BigDecimal fee = fees.pickupBase();
        Double km = distanceKm(center.getLatitude(), center.getLongitude(), address.getLat(), address.getLng());
        if (km != null) {
            fee = fee.add(fees.pickupPerKm().multiply(BigDecimal.valueOf(km)));
        }
        return fee.setScale(3, RoundingMode.HALF_UP);
    }

    private String initialState(FulfillmentMode mode) {
        return mode == FulfillmentMode.PICKUP_DELIVERY ? "PICKUP_SCHEDULED"
                : mode == FulfillmentMode.AT_HOME ? "TECH_ASSIGNED" : null;
    }

    /** Haversine distance in km, or null when either point lacks coordinates. */
    private Double distanceKm(Double lat1, Double lng1, Double lat2, Double lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return null;
        }
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private ServiceAddress copy(ServiceAddress a) {
        return new ServiceAddress(a.getLabel(), a.getGovernorate(), a.getArea(), a.getLat(), a.getLng(), a.getNote());
    }

    // ── Logistics ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public LogisticsResponse getLogistics(User caller, Long bookingId) {
        return toLogisticsResponse(requireOwnedBooking(caller, bookingId));
    }

    /**
     * Center-side: advance the booking's logistics leg. With no target, steps to the next leg; with a
     * target, jumps forward to a named leg (never backward). Authorized via {@code UPDATE_WORK_STAGE}
     * at the booking's center — the same staff who run the job drive its pickup/at-home legs.
     */
    @Transactional
    public LogisticsResponse advanceLogistics(User caller, Long bookingId, String targetState) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingId));
        if (booking.getCenter() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking has no center");
        }
        centerSecurity.requirePermission(booking.getCenter().getId(), caller, CenterPermission.UPDATE_WORK_STAGE);

        FulfillmentMode mode = booking.getFulfillmentMode() != null ? booking.getFulfillmentMode() : FulfillmentMode.DROP_OFF;
        List<String> legs = legsFor(mode);
        if (legs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This booking's fulfillment mode has no logistics legs to advance");
        }
        if (Boolean.TRUE.equals(booking.getFulfillmentDeclined())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Customer declined fulfillment — awaiting a re-choice before logistics can resume");
        }
        int currentIdx = booking.getLogisticsState() != null ? legs.indexOf(booking.getLogisticsState()) : -1;
        int nextIdx;
        if (targetState != null && !targetState.isBlank()) {
            nextIdx = legs.indexOf(targetState);
            if (nextIdx < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown logistics state for this mode");
            }
            if (nextIdx <= currentIdx) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Logistics can only move forward");
            }
        } else {
            if (currentIdx >= legs.size() - 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Already at the final logistics leg");
            }
            nextIdx = currentIdx + 1;
        }
        String previous = booking.getLogisticsState();
        booking.setLogisticsState(legs.get(nextIdx));
        bookingRepository.save(booking);
        log.info("Booking {} logistics advanced {} → {} by user {}", bookingId, previous, legs.get(nextIdx), caller.getId());
        return toLogisticsResponse(booking);
    }

    private List<String> legsFor(FulfillmentMode mode) {
        return switch (mode) {
            case PICKUP_DELIVERY -> PICKUP_LEGS;
            case AT_HOME -> AT_HOME_LEGS;
            default -> List.of();
        };
    }

    private LogisticsResponse toLogisticsResponse(Booking booking) {
        FulfillmentMode mode = booking.getFulfillmentMode() != null ? booking.getFulfillmentMode() : FulfillmentMode.DROP_OFF;
        return new LogisticsResponse(booking.getId(), mode.name(), booking.getLogisticsState(),
                null, Boolean.TRUE.equals(booking.getFulfillmentDeclined()), booking.getFulfillmentDeclineReason(),
                legsFor(mode), LocalDateTime.now().toString());
    }

    @Transactional
    public LogisticsResponse reChoose(User caller, Long bookingId, String mode) {
        Booking booking = requireOwnedBooking(caller, bookingId);
        FulfillmentMode newMode;
        try {
            newMode = FulfillmentMode.valueOf(mode);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown fulfillment mode");
        }
        booking.setFulfillmentMode(newMode);
        booking.setFulfillmentDeclined(false);
        booking.setFulfillmentDeclineReason(null);
        if (newMode == FulfillmentMode.DROP_OFF) {
            booking.setFulfillmentFee(BigDecimal.ZERO);
            booking.setLogisticsState(null);
        } else {
            booking.setFulfillmentFee(computeFee(newMode, booking.getServiceAddress(), booking.getCenter()));
            booking.setLogisticsState(initialState(newMode));
        }
        bookingRepository.save(booking);
        return getLogistics(caller, bookingId);
    }

    // ── Saved addresses (/me/addresses) ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SavedAddressView> listAddresses(User caller) {
        return savedAddressRepository.findByCustomerIdOrderByCreatedAtDesc(caller.getId()).stream()
                .map(this::toView).toList();
    }

    @Transactional
    public SavedAddressView createAddress(User caller, ServiceAddress address) {
        validate(address);
        SavedAddress saved = savedAddressRepository.save(SavedAddress.builder()
                .customer(caller).address(copy(address)).build());
        return toView(saved);
    }

    @Transactional
    public SavedAddressView updateAddress(User caller, Long id, ServiceAddress address) {
        validate(address);
        SavedAddress saved = savedAddressRepository.findByIdAndCustomerId(id, caller.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
        saved.setAddress(copy(address));
        savedAddressRepository.save(saved);
        return toView(saved);
    }

    @Transactional
    public void deleteAddress(User caller, Long id) {
        savedAddressRepository.findByIdAndCustomerId(id, caller.getId())
                .ifPresent(savedAddressRepository::delete);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private void validate(ServiceAddress a) {
        if (a == null || a.getGovernorate() == null || a.getGovernorate().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Governorate is required");
        }
    }

    private Booking requireOwnedBooking(User caller, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingId));
        if (booking.getCustomer() == null || !booking.getCustomer().getId().equals(caller.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your booking");
        }
        return booking;
    }

    /** Resolve the caller's center — direct owner first, else their active staff membership. */
    private MaintenanceCenter resolveMyCenter(User caller) {
        return centerRepository.findFirstByOwnerId(caller.getId())
                .or(() -> membershipRepository
                        .findByUserIdAndStatus(caller.getId(), MembershipStatus.ACTIVE)
                        .stream().findFirst().map(CenterMembership::getCenter))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "No center associated with this account"));
    }

    private SavedAddressView toView(SavedAddress s) {
        ServiceAddress a = s.getAddress();
        return new SavedAddressView(s.getId(), a.getLabel(), a.getGovernorate(), a.getArea(),
                a.getLat(), a.getLng(), a.getNote());
    }
}
