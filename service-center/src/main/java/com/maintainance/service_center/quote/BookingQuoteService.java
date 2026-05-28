package com.maintainance.service_center.quote;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.staff.CenterPermission;
import com.maintainance.service_center.staff.CenterSecurityService;
import com.maintainance.service_center.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

        BigDecimal subtotal = computeSubtotal(request.getLineItems());
        BigDecimal discount = request.getDiscountAmount() != null
                ? request.getDiscountAmount() : BigDecimal.ZERO;

        if (discount.signum() < 0) {
            throw new IllegalArgumentException("Discount cannot be negative");
        }
        if (discount.compareTo(subtotal) > 0) {
            throw new IllegalArgumentException("Discount cannot exceed the subtotal");
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
        }

        List<QuoteLineItem> lineItems = request.getLineItems().stream()
                .map(item -> new QuoteLineItem(
                        item.getDescription(),
                        item.getDescriptionAr(),
                        item.getPartsCost(),
                        item.getLaborCost()))
                .toList();

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

        BookingQuote saved = quoteRepository.save(quote);
        log.info("Sent quote id={} for booking {} by user {}", quoteId, bookingId, caller.getId());
        return BookingQuoteResponse.from(saved);
    }

    private BigDecimal computeSubtotal(List<QuoteLineItemRequest> items) {
        return items.stream()
                .map(item -> item.getPartsCost().add(item.getLaborCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
