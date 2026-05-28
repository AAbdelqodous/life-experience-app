package com.maintainance.service_center.staff;

public enum CenterPermission {
    // Booking management
    MANAGE_BOOKINGS,
    CLAIM_BOOKING,
    ASSIGN_TECHNICIAN_MANUAL,
    VIEW_BOOKING_BASIC,
    VIEW_BOOKINGS_READONLY,
    UPDATE_WORK_STAGE,
    UPLOAD_PROGRESS_MEDIA,

    // Communication
    MANAGE_CHAT,
    RESPOND_REVIEWS,

    // Center management
    EDIT_CENTER_PROFILE,

    // Staff management
    MANAGE_NON_MANAGER_STAFF,
    MANAGE_ALL_STAFF,

    // Financial visibility
    VIEW_REVENUE,
    VIEW_PRICE_LIST,
    MANAGE_PRICING,
    MANAGE_OFFERS,
    VIEW_REPORTS,
    GENERATE_REPORTS,

    // Scheduling
    VIEW_CALENDAR,

    // Staff-specific
    VIEW_ASSIGNED_BOOKINGS,

    // Re-route (spec 022). Names are part of the frontend wire contract
    // (types/staff.ts). No endpoints in this session consume them yet.
    REROUTE_BOOKING_ANY,
    REROUTE_BOOKING_ASSIGNED
}
