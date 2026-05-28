package com.maintainance.service_center.booking;

import lombok.Getter;

/**
 * Thrown by {@link BookingService#claim} when any of the five spec-021 preconditions fail.
 * The {@link ClaimErrorCode} carries both the wire string (frontend reads it from
 * {@code err.data.error}) and the structured {@link com.maintainance.service_center.handler.BusinessErrorCodes}.
 */
@Getter
public class ClaimException extends RuntimeException {

    private final ClaimErrorCode code;

    public ClaimException(ClaimErrorCode code) {
        super(code.name());
        this.code = code;
    }
}
