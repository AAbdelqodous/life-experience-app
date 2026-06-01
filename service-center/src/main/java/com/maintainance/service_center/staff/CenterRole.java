package com.maintainance.service_center.staff;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static com.maintainance.service_center.staff.CenterPermission.*;

public enum CenterRole {
    OWNER(EnumSet.of(
            MANAGE_BOOKINGS, ASSIGN_TECHNICIAN_MANUAL, VIEW_BOOKING_BASIC,
            VIEW_BOOKINGS_READONLY, UPDATE_WORK_STAGE, UPLOAD_PROGRESS_MEDIA,
            MANAGE_CHAT, RESPOND_REVIEWS, EDIT_CENTER_PROFILE,
            MANAGE_NON_MANAGER_STAFF, MANAGE_ALL_STAFF,
            VIEW_REVENUE, VIEW_PRICE_LIST, MANAGE_PRICING, MANAGE_OFFERS,
            VIEW_REPORTS, GENERATE_REPORTS, VIEW_CALENDAR,
            REROUTE_BOOKING_ANY, RESPOND_TO_QUOTES, MANAGE_PAYOUTS,
            MANAGE_INVENTORY, CONSUME_PARTS
    )),

    BRANCH_MANAGER(EnumSet.of(
            MANAGE_BOOKINGS, ASSIGN_TECHNICIAN_MANUAL, MANAGE_CHAT, RESPOND_REVIEWS,
            EDIT_CENTER_PROFILE, MANAGE_NON_MANAGER_STAFF, VIEW_REVENUE, VIEW_REPORTS,
            MANAGE_PRICING, MANAGE_OFFERS,
            REROUTE_BOOKING_ANY, RESPOND_TO_QUOTES, MANAGE_PAYOUTS,
            MANAGE_INVENTORY, CONSUME_PARTS
    )),

    RECEPTIONIST(EnumSet.of(
            MANAGE_BOOKINGS, MANAGE_CHAT, VIEW_CALENDAR, VIEW_BOOKING_BASIC,
            VIEW_PRICE_LIST, MANAGE_PRICING, MANAGE_OFFERS, RESPOND_TO_QUOTES,
            CONSUME_PARTS
    )),

    TECHNICIAN(EnumSet.of(
            CLAIM_BOOKING, VIEW_ASSIGNED_BOOKINGS, UPDATE_WORK_STAGE, UPLOAD_PROGRESS_MEDIA,
            REROUTE_BOOKING_ASSIGNED, CONSUME_PARTS
    )),

    ACCOUNTANT(EnumSet.of(
            VIEW_REVENUE, VIEW_BOOKINGS_READONLY, GENERATE_REPORTS, MANAGE_PAYOUTS
    ));

    private final Set<CenterPermission> permissions;

    CenterRole(Set<CenterPermission> permissions) {
        this.permissions = Collections.unmodifiableSet(permissions);
    }

    public boolean hasPermission(CenterPermission permission) {
        return permissions.contains(permission);
    }

    public Set<CenterPermission> getPermissions() {
        return permissions;
    }

    public String getArabic() {
        return switch (this) {
            case OWNER -> "مالك";
            case BRANCH_MANAGER -> "مدير الفرع";
            case RECEPTIONIST -> "موظف استقبال";
            case TECHNICIAN -> "فني";
            case ACCOUNTANT -> "محاسب";
        };
    }

    public String getEnglish() {
        return switch (this) {
            case OWNER -> "Owner";
            case BRANCH_MANAGER -> "Branch Manager";
            case RECEPTIONIST -> "Receptionist";
            case TECHNICIAN -> "Technician";
            case ACCOUNTANT -> "Accountant";
        };
    }
}
