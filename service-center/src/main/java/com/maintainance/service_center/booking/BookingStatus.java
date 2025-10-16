package com.maintainance.service_center.booking;

public enum BookingStatus {
    PENDING,        // Initial state
    CONFIRMED,      // Center confirmed the booking
    IN_PROGRESS,    // Service being performed
    COMPLETED,      // Service completed
    CANCELLED,      // Booking cancelled
    NO_SHOW,        // Customer didn't show up
    RESCHEDULED     // Booking rescheduled
}
