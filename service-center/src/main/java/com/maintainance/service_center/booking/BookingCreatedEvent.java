package com.maintainance.service_center.booking;

/**
 * Published by {@link BookingService#create} once a booking is persisted, inside the creation
 * transaction. The payment domain listens to apply the center's deposit policy (spec 023) without
 * the booking package depending on it. Synchronous listeners run in the same transaction, so the
 * snapshotted deposit commits atomically with the booking and is visible in the create response.
 */
public record BookingCreatedEvent(Long bookingId) {}
