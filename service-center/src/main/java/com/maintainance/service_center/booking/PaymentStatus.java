package com.maintainance.service_center.booking;

public enum PaymentStatus {
    PENDING,    // Payment has not been processed yet
    PAID,       // Payment has been successfully processed
    FAILED,     // Payment attempt failed
    REFUNDED,   // Payment was refunded to the customer
    CANCELLED,  // Payment was cancelled before completion
    PARTIALLY_REFUNDED, // Partial amount was refunded
    EXPIRED     // Payment link or session expired
}
