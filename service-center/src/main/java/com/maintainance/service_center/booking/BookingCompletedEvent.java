package com.maintainance.service_center.booking;

/**
 * Published by {@link BookingService#complete} once a booking transitions to COMPLETED, inside the
 * completion transaction. Lets other domains react without the booking package depending on them —
 * the payment domain listens to flip its held escrow to release-eligible (spec 023). Synchronous
 * listeners run in the same transaction, so the side effects commit atomically with the completion.
 */
public record BookingCompletedEvent(Long bookingId, Integer completedByUserId) {}
