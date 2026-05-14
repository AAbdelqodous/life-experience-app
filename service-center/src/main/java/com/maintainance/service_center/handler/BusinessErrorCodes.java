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
    SERVICE_TYPE_NOT_ALLOWED_FOR_CENTER(306, HttpStatus.BAD_REQUEST, "Service type does not match any of the center's registered categories");

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
