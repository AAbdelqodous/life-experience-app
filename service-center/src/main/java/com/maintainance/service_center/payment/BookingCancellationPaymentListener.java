package com.maintainance.service_center.payment;

import com.maintainance.service_center.booking.BookingCancelledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Spec 023 — disposes of a cancelled booking's captured funds. The booking package publishes a
 * {@link BookingCancelledEvent}; this synchronous listener runs inside the cancel transaction, so
 * refunds/forfeiture commit atomically with the cancellation. Keeps booking → payment decoupling.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingCancellationPaymentListener {

    private final CenterPaymentService centerPaymentService;

    @EventListener
    public void onBookingCancelled(BookingCancelledEvent event) {
        log.debug("Booking {} cancelled (byCustomer={}) → disposing captured funds",
                event.bookingId(), event.cancelledByCustomer());
        centerPaymentService.handleDepositOnCancellation(event.bookingId(), event.cancelledByCustomer());
    }
}
