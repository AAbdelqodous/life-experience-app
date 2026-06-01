package com.maintainance.service_center.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Center-facing payment views (spec 023). Package-private records. */
final class CenterPaymentViews {
    private CenterPaymentViews() {}
}

/** Earnings dashboard balances, all net of commission, scoped to the active center. */
record CenterBalancesResponse(
        BigDecimal held,
        BigDecimal available,
        BigDecimal paidOut,
        BigDecimal lifetimeGross,
        BigDecimal lifetimeCommission,
        BigDecimal lifetimeNet,
        String currency
) {}

/** One booking's settlement, owner view: gross − commission = net. */
record SettlementResponse(
        Long bookingId,
        List<InvoiceLineDto> lines,
        BigDecimal gross,
        BigDecimal commissionRate,
        BigDecimal commissionAmount,
        BigDecimal refundedAmount,
        BigDecimal net,
        PaymentStatus paymentStatus,
        boolean releaseEligible,
        boolean disputed,
        LocalDateTime autoReleaseAt
) {}

/** Result of marking work complete (makes the customer's escrow release-eligible). */
record MarkCompleteResponse(Long bookingId, boolean releaseEligible, LocalDateTime autoReleaseAt) {}
