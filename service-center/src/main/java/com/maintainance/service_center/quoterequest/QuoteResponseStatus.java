package com.maintainance.service_center.quoterequest;

// Spec 009/024 — state of one center's quote on a request. Absence of a row == NONE (frontend).
public enum QuoteResponseStatus {
    SUBMITTED,
    UPDATED,
    WITHDRAWN,
    SELECTED,
    NOT_SELECTED
}
