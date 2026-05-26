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
    DEPT_MEMBER_WRONG_CENTER(3108, HttpStatus.BAD_REQUEST, "Membership does not belong to the caller's center");

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
