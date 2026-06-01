package com.maintainance.service_center.payment;

/**
 * Spec 023 — distinguishes an upfront {@code DEPOSIT} capture (taken at booking creation to cut
 * no-shows) from the main {@code FULL} payment that settles the balance of the invoice. Both are
 * held in escrow and released together on completion; the deposit is credited against the invoice
 * so the customer is never double-charged. Legacy rows with a null kind are treated as FULL.
 */
public enum PaymentKind { FULL, DEPOSIT }
