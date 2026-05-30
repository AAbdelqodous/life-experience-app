package com.maintainance.service_center.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Response DTOs (views) for the payment/wallet endpoints (spec 007). Package-private records. */
final class PaymentViews {
    private PaymentViews() {}
}

record InvoiceLineDto(String labelEn, String labelAr, BigDecimal amount, String kind) {}

record BookingInvoiceResponse(
        Long bookingId,
        List<InvoiceLineDto> lines,
        BigDecimal total,
        String currency,
        PaymentStatus paymentStatus,
        BigDecimal paidAmount,
        boolean walletApplicable,
        List<PaymentMethod> availableMethods,
        boolean releaseEligible,
        LocalDateTime autoReleaseAt,
        String receiptUrl
) {}

record InitiatePaymentResponseDto(
        Long paymentId,
        PaymentStatus status,
        String checkoutUrl,
        String returnUrlPrefix
) {}

record PaymentStatusResponseDto(Long paymentId, PaymentStatus status, Long bookingId) {}

record ReleaseResponseDto(Long bookingId, PaymentStatus paymentStatus) {}

record DisputeResponseDto(Long bookingId, PaymentStatus paymentStatus, boolean disputed) {}

record SavedMethodResponseDto(Long id, String brand, String maskedLabel, String expiry) {}

record WalletResponseDto(BigDecimal balance, String currency) {}

record WalletTransactionResponseDto(
        Long id,
        String type,
        BigDecimal amount,
        Long bookingId,
        LocalDateTime createdAt,
        String descriptionEn,
        String descriptionAr
) {}
