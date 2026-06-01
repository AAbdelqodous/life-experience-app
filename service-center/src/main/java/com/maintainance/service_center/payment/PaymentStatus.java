package com.maintainance.service_center.payment;

// Spec 007/023 — escrow/payment state machine.
public enum PaymentStatus {
    PENDING,   // no successful capture yet
    HELD,      // captured, held in escrow, awaiting release
    RELEASED,  // released by customer/auto, settling to center
    PAID,      // fully settled (or wallet-covered)
    REFUNDED,
    FAILED
}
