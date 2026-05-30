package com.maintainance.service_center.payment;

// Spec 007 — payment methods (gateway-hosted; Kuwait-first). Distinct from booking.PaymentMethod.
public enum PaymentMethod {
    KNET,
    CARD,
    APPLE_PAY,
    GOOGLE_PAY,
    WALLET
}
