package com.maintainance.service_center.quote;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.user.User;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingQuoteService {

    private final BookingQuoteRepository quoteRepository;
    private final BookingRepository bookingRepository;
    private final MaintenanceCenterRepository centerRepository;

    public List<BookingQuoteResponse> getBookingQuotes(User owner, Long bookingId) {
        getBookingForOwner(bookingId, owner);
        return quoteRepository.findByBookingIdOrderByVersionDesc(bookingId).stream()
                .map(BookingQuoteResponse::from)
                .toList();
    }

    @Transactional
    public BookingQuoteResponse createQuote(User owner, Long bookingId, CreateQuoteRequest request) {
        Booking booking = getBookingForOwner(bookingId, owner);

        int version = quoteRepository.countByBookingId(bookingId) + 1;

        BigDecimal subtotal = computeSubtotal(request.getLineItems());
        BigDecimal discount = request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal totalAmount = computeTotal(subtotal, discount);

        List<QuoteLineItem> lineItems = request.getLineItems().stream()
                .map(itemRequest -> {
                    QuoteLineItem item = new QuoteLineItem();
                    item.setDescription(itemRequest.getDescription());
                    item.setDescriptionAr(itemRequest.getDescriptionAr());
                    item.setPartsCost(itemRequest.getPartsCost());
                    item.setLaborCost(itemRequest.getLaborCost());
                    return item;
                })
                .toList();

        BookingQuote quote = new BookingQuote();
        quote.setBooking(booking);
        quote.setVersion(version);
        quote.setLineItems(lineItems);
        quote.setDiscountAmount(discount);
        quote.setDiscountReason(request.getDiscountReason());
        quote.setTaxAmount(BigDecimal.ZERO);
        quote.setSubtotal(subtotal);
        quote.setTotalAmount(totalAmount);
        quote.setEstimatedDurationMinutes(request.getEstimatedDurationMinutes());
        quote.setNotes(request.getNotes());
        quote.setNotesAr(request.getNotesAr());
        quote.setStatus(QuoteStatus.DRAFT);

        BookingQuote saved = quoteRepository.save(quote);
        log.info("Created quote version {} for booking {} by owner {}", version, bookingId, owner.getId());
        return BookingQuoteResponse.from(saved);
    }

    @Transactional
    public BookingQuoteResponse sendQuote(User owner, Long bookingId, Long quoteId) {
        getBookingForOwner(bookingId, owner);

        BookingQuote quote = quoteRepository.findByIdAndBookingId(quoteId, bookingId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Quote not found with id: " + quoteId + " for booking: " + bookingId));

        if (quote.getStatus() != QuoteStatus.DRAFT) {
            throw new IllegalStateException(
                    "Quote is not in DRAFT status. Current status: " + quote.getStatus());
        }

        quote.setStatus(QuoteStatus.SENT);
        quote.setSentAt(LocalDateTime.now());

        BookingQuote saved = quoteRepository.save(quote);
        log.info("Sent quote {} for booking {} by owner {}", quoteId, bookingId, owner.getId());
        return BookingQuoteResponse.from(saved);
    }

    private Booking getBookingForOwner(Long bookingId, User owner) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found with id: " + bookingId));

        MaintenanceCenter center = centerRepository.findFirstByOwnerId(owner.getId())
                .orElseThrow(() -> new EntityNotFoundException("Maintenance center not found for owner"));

        if (!booking.getCenter().getId().equals(center.getId())) {
            throw new AccessDeniedException("You do not have access to this booking");
        }

        return booking;
    }

    private BigDecimal computeSubtotal(List<QuoteLineItemRequest> items) {
        return items.stream()
                .map(item -> item.getPartsCost().add(item.getLaborCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal computeTotal(BigDecimal subtotal, BigDecimal discount) {
        BigDecimal total = subtotal.subtract(discount);
        return total.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : total;
    }
}
