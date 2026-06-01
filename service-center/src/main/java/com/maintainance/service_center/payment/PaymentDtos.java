package com.maintainance.service_center.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Request DTOs for the payment/wallet endpoints (spec 007). Package-private records. */
final class PaymentDtos {
    private PaymentDtos() {}
}

record InitiatePaymentRequestDto(
        @NotNull Long bookingId,
        @NotNull PaymentMethod method,
        boolean useWalletBalance,
        Boolean saveCard,
        Long savedMethodId,
        @NotBlank String idempotencyKey
) {}

record TopUpRequestDto(
        @NotNull @DecimalMin(value = "0.001") BigDecimal amount,
        @NotNull PaymentMethod method,
        @NotBlank String idempotencyKey
) {}

record DisputeRequestDto(String reason) {}
