package com.maintainance.service_center.booking;

public enum CancelledBy {
    CUSTOMER,  // When the customer cancels the booking
    CENTER,    // When the maintenance center cancels the booking
    ADMIN,     // When an admin or staff member cancels the booking
    SYSTEM     // When the system automatically cancels the booking (e.g., due to timeout)
}
