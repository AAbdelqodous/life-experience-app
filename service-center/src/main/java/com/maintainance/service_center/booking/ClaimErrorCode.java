package com.maintainance.service_center.booking;

import com.maintainance.service_center.handler.BusinessErrorCodes;
import lombok.Getter;

/**
 * Wire-contract error codes for the self-claim flow (spec 021 §7).
 * <p>The enum constant name (e.g. {@code STAFF_INACTIVE}) IS the string the frontend
 * reads from {@code err.data.error}. Do not rename. The numeric {@link BusinessErrorCodes}
 * carries the structured representation returned alongside.
 */
@Getter
public enum ClaimErrorCode {
    STAFF_INACTIVE(BusinessErrorCodes.SC_STAFF_INACTIVE),
    BOOKING_NOT_CLAIMABLE(BusinessErrorCodes.SC_BOOKING_NOT_CLAIMABLE),
    BOOKING_ALREADY_CLAIMED(BusinessErrorCodes.SC_BOOKING_ALREADY_CLAIMED),
    WRONG_DEPARTMENT(BusinessErrorCodes.SC_WRONG_DEPARTMENT);

    private final BusinessErrorCodes businessErrorCode;

    ClaimErrorCode(BusinessErrorCodes businessErrorCode) {
        this.businessErrorCode = businessErrorCode;
    }
}
