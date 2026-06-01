package com.maintainance.service_center.payment;

import com.maintainance.service_center.booking.BookingCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Spec 023 — folds the escrow "mark complete" step into the booking-lifecycle completion. When a
 * booking is completed, {@link com.maintainance.service_center.booking.BookingService} publishes a
 * {@link BookingCompletedEvent}; this synchronous listener runs inside the same completion
 * transaction, so flipping the held payment to release-eligible commits atomically with the
 * completion (no separate authenticated endpoint, no double bookkeeping).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingCompletionPaymentListener {

    private final CenterPaymentService centerPaymentService;

    @EventListener
    public void onBookingCompleted(BookingCompletedEvent event) {
        log.debug("Booking {} completed → marking escrow release-eligible", event.bookingId());
        centerPaymentService.markReleaseEligible(event.bookingId());
    }
}
