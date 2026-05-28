package com.maintainance.service_center.reroute;

/**
 * Structured reasons for a re-route operation (spec 022 §4 Glossary, FR-DR-015).
 * Names are the wire contract — they appear in audit responses and the frontend's
 * {@code types/reroute.ts RerouteReason} union.
 */
public enum RerouteReason {
    WRONG_DIAGNOSIS,
    OUT_OF_SCOPE,
    SPECIALIST_NEEDED,
    STAFF_UNAVAILABLE,
    CUSTOMER_REQUEST,
    OTHER
}
