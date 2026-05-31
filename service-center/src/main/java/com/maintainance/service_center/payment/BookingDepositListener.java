package com.maintainance.service_center.payment;

import com.maintainance.service_center.booking.BookingCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Spec 023 — applies the center's deposit policy when a booking is created. The booking package
 * publishes a {@link BookingCreatedEvent}; this synchronous listener runs inside the creation
 * transaction, so the snapshotted deposit commits atomically with the booking and appears in the
 * create response. Keeps booking → payment decoupling (payment listens, booking never imports it).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingDepositListener {

    private final CenterPaymentService centerPaymentService;

    @EventListener
    public void onBookingCreated(BookingCreatedEvent event) {
        log.debug("Booking {} created → applying center deposit policy", event.bookingId());
        centerPaymentService.applyDepositOnCreation(event.bookingId());
    }
}
