package com.maintainance.service_center.handler;

import lombok.Getter;
import org.springframework.http.HttpStatus;

public enum BusinessErrorCodes {
    // General errors
    NO_CODE(0, HttpStatus.NOT_IMPLEMENTED, "No code"),
    INCORRECT_CURRENT_PASSWORD(300, HttpStatus.BAD_REQUEST, "Incorrect password"),
    NEW_PASSWORD_DOES_NOT_MATCH(301, HttpStatus.BAD_REQUEST, "Password mismatch"),
    ACCOUNT_LOCKED(302, HttpStatus.FORBIDDEN, "Account has been locked"),
    ACCOUNT_DISABLED( 303, HttpStatus.FORBIDDEN, "Account is disabled"),
    BAD_CREDENTIALS( 304, HttpStatus.FORBIDDEN, "Username and / or password is incorrect"),
    ACCOUNT_REJECTED(305, HttpStatus.FORBIDDEN, "Account has been rejected by the platform administrator / تم رفض الحساب من قبل إدارة المنصة"),
    SERVICE_TYPE_NOT_ALLOWED_FOR_CENTER(306, HttpStatus.BAD_REQUEST, "Service type does not match any of the center's registered categories"),

    // Staff & permission errors (3001–3007)
    STAFF_LIMIT_REACHED(3001, HttpStatus.BAD_REQUEST, "Staff limit reached"),
    CANNOT_INVITE_SELF(3002, HttpStatus.BAD_REQUEST, "Cannot invite yourself"),
    CANNOT_INVITE_ADMIN(3003, HttpStatus.BAD_REQUEST, "Admins cannot be invited as staff"),
    INVITATION_NOT_PENDING(3004, HttpStatus.BAD_REQUEST, "Invitation is not in pending status"),
    INVITATION_EMAIL_MISMATCH(3005, HttpStatus.FORBIDDEN, "This invitation was sent to a different email address"),
    CANNOT_REMOVE_OWNER(3006, HttpStatus.BAD_REQUEST, "Cannot remove the center owner"),
    INSUFFICIENT_ROLE_SCOPE(3007, HttpStatus.FORBIDDEN, "Insufficient role scope to perform this action"),

    // Department errors (3100–3199) — spec 020
    DEPT_DUPLICATE_NAME_AR(3100, HttpStatus.BAD_REQUEST, "A department with this Arabic name already exists at this center"),
    DEPT_DUPLICATE_NAME_EN(3101, HttpStatus.BAD_REQUEST, "A department with this English name already exists at this center"),
    DEPT_INVALID_CATEGORY(3102, HttpStatus.BAD_REQUEST, "One or more selected categories do not exist"),
    DEPT_HAS_OPEN_BOOKINGS(3103, HttpStatus.CONFLICT, "Department has open bookings; reassign or complete them before deactivating"),
    DEPT_HAS_ACTIVE_MEMBERS(3104, HttpStatus.CONFLICT, "Department has active technician members; remove them before deactivating"),
    DEPT_LAST_ACTIVE(3105, HttpStatus.CONFLICT, "Cannot deactivate the last active department; every center must have at least one"),
    DEPT_MEMBER_NOT_TECHNICIAN(3106, HttpStatus.BAD_REQUEST, "Only technicians can be assigned to a department"),
    DEPT_MEMBER_ALREADY_ASSIGNED(3107, HttpStatus.BAD_REQUEST, "Membership already assigned to this department"),
    DEPT_MEMBER_WRONG_CENTER(3108, HttpStatus.BAD_REQUEST, "Membership does not belong to the caller's center"),

    // Self-claim errors (3200–3299) — spec 021. Wire codes (string names below) MUST match the spec §7 contract.
    SC_STAFF_INACTIVE(3200, HttpStatus.CONFLICT, "Your membership is no longer active at this center"),
    SC_BOOKING_NOT_CLAIMABLE(3201, HttpStatus.CONFLICT, "This booking is no longer available for claim"),
    SC_BOOKING_ALREADY_CLAIMED(3202, HttpStatus.CONFLICT, "This booking was just claimed by another technician"),
    SC_WRONG_DEPARTMENT(3203, HttpStatus.CONFLICT, "This booking is in a different department"),

    // Diagnostic + reroute errors (3300–3399) — spec 022. Wire `error` field carries the enum name.
    DUPLICATE_DIAGNOSTIC_DEPARTMENT(3300, HttpStatus.CONFLICT, "Another active department at this center is already flagged as diagnostic"),
    INVALID_DIAGNOSTIC_FEE_TARGET(3301, HttpStatus.BAD_REQUEST, "Diagnostic fee can only be set on a department flagged as diagnostic"),
    DIAGNOSTIC_TOGGLE_BLOCKED_BY_OPEN_BOOKINGS(3302, HttpStatus.CONFLICT, "Cannot toggle the diagnostic flag while the department has open bookings"),
    FORBIDDEN_REROUTE(3303, HttpStatus.FORBIDDEN, "You do not have permission to re-route this booking"),
    CANNOT_REROUTE_INTO_DIAGNOSTIC(3304, HttpStatus.BAD_REQUEST, "Bookings cannot be re-routed into the diagnostic department"),
    NO_OP_REROUTE(3305, HttpStatus.BAD_REQUEST, "Cannot re-route to the same department"),
    INVALID_BOOKING_STATUS_FOR_REROUTE(3306, HttpStatus.BAD_REQUEST, "This booking cannot be re-routed in its current status"),
    REROUTE_CONFLICT(3307, HttpStatus.CONFLICT, "Another change was applied to this booking; refresh and retry"),
    INVALID_REROUTE_REASON(3308, HttpStatus.BAD_REQUEST, "Re-route reason must be one of the allowed values"),
    NOTE_TOO_LONG(3309, HttpStatus.BAD_REQUEST, "Re-route note may not exceed 500 characters"),
    DIAGNOSTIC_FEE_LOCKED(3310, HttpStatus.BAD_REQUEST, "Diagnostic fee line item cannot be removed or edited");

    @Getter
    private final int code;
    @Getter
    private final HttpStatus httpStatus;
    @Getter
    private final String description;

    BusinessErrorCodes(int code, HttpStatus httpStatus, String description) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.description = description;
    }
}
