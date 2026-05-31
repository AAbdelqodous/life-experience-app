package com.maintainance.service_center.quote;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.inventory.InventoryService;
import com.maintainance.service_center.inventory.Part;
import com.maintainance.service_center.staff.CenterPermission;
import com.maintainance.service_center.staff.CenterSecurityService;
import com.maintainance.service_center.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingQuoteService {

    /** Non-terminal statuses whose prior versions get flipped to REVISED when a new draft is created. */
    private static final Set<QuoteStatus> REVISABLE_FROM =
            Set.of(QuoteStatus.DRAFT, QuoteStatus.SENT, QuoteStatus.REJECTED);

    private final BookingQuoteRepository quoteRepository;
    private final BookingRepository bookingRepository;
    private final CenterSecurityService centerSecurity;
    private final InventoryService inventoryService;

    public List<BookingQuoteResponse> getBookingQuotes(User caller, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found with id: " + bookingId));
        centerSecurity.requireActiveMembership(booking.getCenter().getId(), caller);
        return quoteRepository.findByBookingIdOrderByVersionDesc(bookingId).stream()
                .map(BookingQuoteResponse::from)
                .toList();
    }

    @Transactional
    public BookingQuoteResponse createQuote(User caller, Long bookingId, CreateQuoteRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found with id: " + bookingId));
        // Quotes are a manager/receptionist action — assigned techs do not write quotes.
        // MANAGE_PRICING is the closest permission (OWNER, BRANCH_MANAGER, RECEPTIONIST).
        centerSecurity.requirePermission(
                booking.getCenter().getId(), caller, CenterPermission.MANAGE_PRICING);

        // Spec 025 — map user lines first, snapshotting catalogued-part prices (salePrice×qty, R4),
        // and derive the subtotal from the snapshotted lines so the total is server-authoritative.
        Long centerId = booking.getCenter().getId();
        List<QuoteLineItem> userLines = new ArrayList<>();
        request.getLineItems().forEach(item -> userLines.add(toLineItem(item, centerId)));
        BigDecimal subtotal = userLines.stream()
                .map(l -> l.getPartsCost().add(l.getLaborCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = request.getDiscountAmount() != null
                ? request.getDiscountAmount() : BigDecimal.ZERO;

        if (discount.signum() < 0) {
            throw new IllegalArgumentException("Discount cannot be negative");
        }
        if (discount.compareTo(subtotal) > 0) {
            throw new IllegalArgumentException("Discount cannot exceed the subtotal");
        }

        // Spec 022 FR-DR-010: auto-add a non-removable DIAGNOSTIC_FEE line item when the
        // booking passed through diagnostic AND a fee snapshot was captured at claim. The
        // fee folds into the subtotal so the customer-facing total is correct end-to-end
        // (FR-DR-011). The check is on the snapshot, not the current dept rate, so owner
        // edits to the fee after claim do not retroactively change owed amount (FR-DR-014).
        boolean addDiagnosticFee = Boolean.TRUE.equals(booking.getPassedThroughDiagnostic())
                && booking.getDiagnosticFeeRateAtClaim() != null
                && booking.getDiagnosticFeeRateAtClaim().signum() > 0;
        BigDecimal diagnosticFee = addDiagnosticFee
                ? booking.getDiagnosticFeeRateAtClaim() : BigDecimal.ZERO;
        if (addDiagnosticFee) {
            subtotal = subtotal.add(diagnosticFee);
        }

        BigDecimal tax = BigDecimal.ZERO; // Kuwait has no VAT today; tax computed server-side per spec.
        BigDecimal totalAmount = subtotal.subtract(discount).add(tax);

        // Spec 009 FR-018 / DoD step 4: creating a new version flips any non-terminal prior
        // version (DRAFT, SENT, REJECTED) to REVISED. APPROVED quotes are kept untouched —
        // once approved, the original record is preserved as the source of truth.
        int nextVersion = 1;
        List<BookingQuote> existing = quoteRepository.findByBookingIdOrderByVersionDesc(bookingId);
        if (!existing.isEmpty()) {
            nextVersion = existing.get(0).getVersion() + 1;
            for (BookingQuote prior : existing) {
                if (REVISABLE_FROM.contains(prior.getStatus())) {
                    prior.setStatus(QuoteStatus.REVISED);
                    quoteRepository.save(prior);
                }
            }
            // Spec 025 (R5): superseding prior quote versions returns any stock those versions
            // consumed; the new version re-consumes on its own send. Net-based, so it's a no-op
            // when nothing was consumed.
            inventoryService.reverseConsumption(bookingId, caller);
        }

        // Spec 022: prepend the system-generated DIAGNOSTIC_FEE line item before user lines
        // so it always renders first. partsCost=0, laborCost=snapshot (data-model.md
        // §BookingQuote extension — "If only parts/labor exist, populate laborCost = fee").
        // The kind discriminator + QuoteLineItemResponse.from() make this row immutable
        // on the wire (editable=false, removable=false, descriptionKey set).
        List<QuoteLineItem> lineItems = new ArrayList<>();
        if (addDiagnosticFee) {
            QuoteLineItem feeLine = new QuoteLineItem();
            feeLine.setDescription("Diagnostic Fee"); // frontend resolves descriptionKey instead
            feeLine.setDescriptionAr("رسوم التشخيص");
            feeLine.setPartsCost(BigDecimal.ZERO);
            feeLine.setLaborCost(diagnosticFee);
            feeLine.setKind(QuoteLineItemKind.DIAGNOSTIC_FEE);
            lineItems.add(feeLine);
        }
        lineItems.addAll(userLines); // already snapshotted above

        BookingQuote quote = new BookingQuote();
        quote.setBooking(booking);
        quote.setVersion(nextVersion);
        quote.setLineItems(lineItems);
        quote.setDiscountAmount(discount);
        quote.setDiscountReason(request.getDiscountReason());
        quote.setTaxAmount(tax);
        quote.setSubtotal(subtotal);
        quote.setTotalAmount(totalAmount);
        quote.setEstimatedDurationMinutes(request.getEstimatedDurationMinutes());
        quote.setNotes(request.getNotes());
        quote.setNotesAr(request.getNotesAr());
        quote.setStatus(QuoteStatus.DRAFT);

        BookingQuote saved = quoteRepository.save(quote);
        log.info("Created quote id={} version={} for booking {} by user {}",
                saved.getId(), nextVersion, bookingId, caller.getId());
        return BookingQuoteResponse.from(saved);
    }

    @Transactional
    public BookingQuoteResponse sendQuote(User caller, Long bookingId, Long quoteId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found with id: " + bookingId));
        centerSecurity.requirePermission(
                booking.getCenter().getId(), caller, CenterPermission.MANAGE_PRICING);

        BookingQuote quote = quoteRepository.findByIdAndBookingId(quoteId, bookingId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Quote not found with id: " + quoteId + " for booking: " + bookingId));

        // Spec 009 contract: non-DRAFT send → 400 (validation), not 409 (server state).
        if (quote.getStatus() != QuoteStatus.DRAFT) {
            throw new IllegalArgumentException(
                    "Quote is not in DRAFT status; current status: " + quote.getStatus());
        }

        quote.setStatus(QuoteStatus.SENT);
        quote.setSentAt(LocalDateTime.now());

        // Spec 025 — sending the quote commits its catalogued part lines: decrement stock atomically
        // and record a booking-linked CONSUME movement (R3). Ad-hoc/labor lines have no stock effect.
        Long centerId = booking.getCenter().getId();
        for (QuoteLineItem line : quote.getLineItems()) {
            if (line.getPartId() != null && !line.isAdHoc()) {
                int qty = line.getQuantity() != null && line.getQuantity() > 0 ? line.getQuantity() : 1;
                inventoryService.consume(centerId, line.getPartId(), qty, booking, caller);
            }
        }

        BookingQuote saved = quoteRepository.save(quote);
        log.info("Sent quote id={} for booking {} by user {}", quoteId, bookingId, caller.getId());
        return BookingQuoteResponse.from(saved);
    }

    /** Spec 025 — map a request line, snapshotting a catalogued part's salePrice×quantity (R4). */
    private QuoteLineItem toLineItem(QuoteLineItemRequest item, Long centerId) {
        QuoteLineItem line = new QuoteLineItem();
        line.setDescription(item.getDescription());
        line.setDescriptionAr(item.getDescriptionAr());
        line.setLaborCost(item.getLaborCost());
        line.setKind(null); // user line — editable/removable
        if (item.getPartId() != null && !item.isAdHoc()) {
            int qty = (item.getQuantity() != null && item.getQuantity() > 0) ? item.getQuantity() : 1;
            Part part = inventoryService.findActivePart(centerId, item.getPartId());
            if (part == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Part not available in this center's catalog");
            }
            line.setPartId(part.getId());
            line.setQuantity(qty);
            line.setAdHoc(false);
            line.setPartsCost(part.getSalePrice().multiply(BigDecimal.valueOf(qty))); // R4 price snapshot
        } else {
            line.setPartId(item.getPartId());
            line.setQuantity(item.getQuantity());
            line.setAdHoc(item.isAdHoc());
            line.setPartsCost(item.getPartsCost());
        }
        return line;
    }

    /**
     * Spec 022 FR-DR-021: mark any in-force quote (status SENT or APPROVED) for this booking
     * as REVISED. Used by the re-route flow so the customer is asked to re-approve a fresh
     * quote built by the technician at the booking's new department.
     * <p>Returns {@code true} if any quote was flipped (i.e. an active quote existed at
     * re-route time), {@code false} otherwise. The caller uses this to vary the customer
     * notification body (FR-DR-029).
     * <p>Mandatory propagation: this MUST be called from within the re-route transaction.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public boolean markRevisedByBookingId(Long bookingId) {
        List<BookingQuote> active = quoteRepository.findByBookingIdOrderByVersionDesc(bookingId)
                .stream()
                .filter(q -> q.getStatus() == QuoteStatus.SENT
                        || q.getStatus() == QuoteStatus.APPROVED)
                .toList();
        for (BookingQuote q : active) {
            q.setStatus(QuoteStatus.REVISED);
            quoteRepository.save(q);
        }
        if (!active.isEmpty()) {
            log.info("Marked {} active quote(s) as REVISED for booking {}", active.size(), bookingId);
        }
        return !active.isEmpty();
    }
}
