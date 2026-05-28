package com.maintainance.service_center.reroute;

import com.maintainance.service_center.handler.BusinessErrorCodes;
import lombok.Getter;

/**
 * Thrown by {@link RerouteService} when any spec 022 re-route validation fails.
 * Carries a {@link BusinessErrorCodes} so {@code GlobalExceptionHandling} can map
 * it to the correct HTTP status + wire {@code error} code.
 */
@Getter
public class RerouteException extends RuntimeException {

    private final BusinessErrorCodes errorCode;

    public RerouteException(BusinessErrorCodes errorCode) {
        super(errorCode.getDescription());
        this.errorCode = errorCode;
    }

    public RerouteException(BusinessErrorCodes errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
