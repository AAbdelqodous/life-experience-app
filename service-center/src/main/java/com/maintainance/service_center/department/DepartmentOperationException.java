package com.maintainance.service_center.department;

import com.maintainance.service_center.handler.BusinessErrorCodes;
import lombok.Getter;

@Getter
public class DepartmentOperationException extends RuntimeException {

    private final BusinessErrorCodes errorCode;

    public DepartmentOperationException(BusinessErrorCodes errorCode) {
        super(errorCode.getDescription());
        this.errorCode = errorCode;
    }

    public DepartmentOperationException(BusinessErrorCodes errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}