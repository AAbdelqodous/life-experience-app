package com.maintainance.service_center.handler;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final BusinessErrorCodes errorCode;

    public BusinessException(BusinessErrorCodes errorCode) {
        super(errorCode.getDescription());
        this.errorCode = errorCode;
    }
}
