package com.maintainance.service_center.booking;

/**
 * Published by {@link BookingService#cancel} once a booking is cancelled, inside the cancel
 * transaction. The payment domain listens to dispose of any captured funds (spec 023): the balance
 * is always refunded (work not done), and the deposit is forfeited to the center only when the
 * customer cancelled under a RETAIN policy — otherwise it is refunded. Booking stays decoupled from
 * payment; a synchronous listener commits the disposition atomically with the cancellation.
 */
public record BookingCancelledEvent(Long bookingId, boolean cancelledByCustomer) {}
