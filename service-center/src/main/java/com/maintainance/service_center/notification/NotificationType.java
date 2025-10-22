package com.maintainance.service_center.notification;

public enum NotificationType {
    // Booking related
    BOOKING_CONFIRMED,
    BOOKING_CANCELLED,
    BOOKING_REMINDER,
    BOOKING_RESCHEDULED,
    BOOKING_COMPLETED,

    // Service related
    SERVICE_STARTED,
    SERVICE_COMPLETED,
    SERVICE_UPDATE,

    // Review related
    NEW_REVIEW,
    REVIEW_RESPONSE,
    REVIEW_REQUEST,

    // Message related
    NEW_MESSAGE,

    // Payment related
    PAYMENT_DUE,
    PAYMENT_RECEIVED,
    PAYMENT_FAILED,

    // Account related
    ACCOUNT_VERIFIED,
    PASSWORD_RESET,
    PROFILE_UPDATE,

    // Promotional
    PROMOTION,
    DISCOUNT,
    NEW_CENTER_NEARBY,

    // System
    SYSTEM_MAINTENANCE,
    SYSTEM_UPDATE,
    GENERAL_ANNOUNCEMENT
}