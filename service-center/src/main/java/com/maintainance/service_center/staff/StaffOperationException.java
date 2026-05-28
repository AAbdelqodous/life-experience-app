package com.maintainance.service_center.staff;

import com.maintainance.service_center.handler.BusinessErrorCodes;
import lombok.Getter;

@Getter
public class StaffOperationException extends RuntimeException {

    private final BusinessErrorCodes errorCode;

    public StaffOperationException(BusinessErrorCodes errorCode) {
        super(errorCode.getDescription());
        this.errorCode = errorCode;
    }

    public StaffOperationException(BusinessErrorCodes errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
