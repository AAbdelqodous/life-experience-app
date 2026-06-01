package com.maintainance.service_center.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Spec 023 — deposit / refund / payout DTOs. Package-private records. */
final class CenterPayoutDtos {
    private CenterPayoutDtos() {}
}

// ── Deposits ──
record DepositConfigResponse(
        String mode, BigDecimal flatAmount, Integer percent,
        Long appliesToServiceId, String cancellationPolicy) {}

record UpdateDepositConfigRequest(
        @NotNull DepositMode mode, BigDecimal flatAmount, Integer percent,
        Long appliesToServiceId, CancellationPolicy cancellationPolicy) {}

// ── Refunds ──
record RefundRequestDto(
        @NotNull @DecimalMin(value = "0.001") BigDecimal amount,
        String target, String reason) {}

record RefundResponse(Long bookingId, PaymentStatus paymentStatus, BigDecimal refundedAmount, BigDecimal net) {}

// ── Payout account ──
record UpsertPayoutAccountRequest(@NotBlank String iban, @NotBlank String holderName) {}

record PayoutAccountResponse(Long id, String iban, String holderName, String bankName, String status) {}

// ── Payouts ──
record RequestPayoutRequest(@NotNull @DecimalMin(value = "0.001") BigDecimal amount, @NotNull Long accountId) {}

record PayoutResponse(
        Long id, BigDecimal amount, Long accountId, String status, String reference,
        LocalDateTime requestedAt, LocalDateTime completedAt, String failureReason) {}
