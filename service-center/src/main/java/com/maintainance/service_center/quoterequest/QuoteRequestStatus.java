package com.maintainance.service_center.quoterequest;

// Spec 009/024 — lifecycle of a customer quote request.
public enum QuoteRequestStatus {
    OPEN,
    ACCEPTED,
    EXPIRED,
    CANCELLED
}
