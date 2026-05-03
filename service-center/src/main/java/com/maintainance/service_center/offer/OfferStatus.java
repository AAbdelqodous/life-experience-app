package com.maintainance.service_center.offer;

/** Computed from dates and cancelledAt — never stored in the database. */
public enum OfferStatus {
    SCHEDULED,
    ACTIVE,
    EXPIRED,
    CANCELLED
}
