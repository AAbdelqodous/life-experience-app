package com.maintainance.service_center.payment;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.quote.BookingQuote;
import com.maintainance.service_center.quote.BookingQuoteRepository;
import com.maintainance.service_center.quote.QuoteLineItem;
import com.maintainance.service_center.quote.QuoteLineItemKind;
import com.maintainance.service_center.quote.QuoteStatus;
import com.maintainance.service_center.staff.CenterMembership;
import com.maintainance.service_center.staff.CenterMembershipRepository;
import com.maintainance.service_center.staff.CenterPermission;
import com.maintainance.service_center.staff.CenterSecurityService;
import com.maintainance.service_center.staff.MembershipStatus;
import com.maintainance.service_center.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Spec 023 — center-facing payments: mark complete (release trigger), earnings, settlement,
 * deposit config, and refunds. Reuses the {@link Payment} domain. Payouts live in
 * {@link CenterPayoutService}; earnings here account for refunds and (via that service) payouts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CenterPaymentService {

    private static final long AUTO_RELEASE_HOURS = 72;
    private static final Set<PaymentStatus> CAPTURED =
            EnumSet.of(PaymentStatus.HELD, PaymentStatus.RELEASED, PaymentStatus.PAID);
    private static final Set<PaymentStatus> SETTLED =
            EnumSet.of(PaymentStatus.RELEASED, PaymentStatus.PAID);

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingQuoteRepository quoteRepository;
    private final MaintenanceCenterRepository centerRepository;
    private final CenterMembershipRepository membershipRepository;
    private final CenterSecurityService centerSecurity;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTxRepository;
    private final DepositConfigRepository depositConfigRepository;
    private final PayoutRepository payoutRepository;

    // ── Mark complete (release trigger) ───────────────────────────────────────

    /**
     * Flags the booking's held payment as release-eligible (and starts the auto-release clock).
     * This is the escrow side of completing a booking — it is folded into the booking-lifecycle
     * completion: {@code BookingService.complete} publishes a {@code BookingCompletedEvent} that
     * {@link BookingCompletionPaymentListener} forwards here, inside the same transaction. There is
     * no separate authenticated endpoint; the completion call already enforced center permissions.
     */
    @Transactional
    public MarkCompleteResponse markReleaseEligible(Long bookingId) {
        Payment latest = paymentRepository.findByBookingIdOrderByCreatedAtDesc(bookingId)
                .stream().findFirst().orElse(null);
        if (latest == null) {
            // No escrow payment (e.g. cash booking) — nothing to release, completion still succeeds.
            return new MarkCompleteResponse(bookingId, true, null);
        }
        if (latest.getStatus() != PaymentStatus.HELD) {
            // Already released/refunded/pending — leave it; only a HELD payment becomes eligible.
            return new MarkCompleteResponse(bookingId, latest.isReleaseEligible(), latest.getAutoReleaseAt());
        }
        latest.setReleaseEligible(true);
        if (latest.getAutoReleaseAt() == null) {
            latest.setAutoReleaseAt(LocalDateTime.now().plusHours(AUTO_RELEASE_HOURS));
        }
        paymentRepository.save(latest);
        log.info("Booking {} completed → payment {} release-eligible (auto-release at {})",
                bookingId, latest.getId(), latest.getAutoReleaseAt());
        return new MarkCompleteResponse(bookingId, true, latest.getAutoReleaseAt());
    }

    // ── Earnings dashboard ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CenterBalancesResponse getEarnings(User caller) {
        MaintenanceCenter center = resolveMyCenter(caller);
        centerSecurity.requirePermission(center.getId(), caller, CenterPermission.VIEW_REVENUE);

        List<Payment> payments = paymentRepository.findByCenterId(center.getId());
        BigDecimal held = BigDecimal.ZERO, releasedNet = BigDecimal.ZERO;
        BigDecimal gross = BigDecimal.ZERO, commission = BigDecimal.ZERO, net = BigDecimal.ZERO;
        for (Payment p : payments) {
            if (!CAPTURED.contains(p.getStatus())) continue;
            BigDecimal effNet = nz(p.getNetAmount()).subtract(nz(p.getRefundedAmount()));
            gross = gross.add(nz(p.getGrossAmount()));
            commission = commission.add(nz(p.getCommissionAmount()));
            net = net.add(effNet);
            if (p.getStatus() == PaymentStatus.HELD) held = held.add(effNet);
            else releasedNet = releasedNet.add(effNet);
        }
        // Available = settled net − payouts already requested/processing/paid (spec 023).
        BigDecimal payoutsTaken = payoutRepository.findByCenterIdOrderByRequestedAtDesc(center.getId()).stream()
                .filter(po -> po.getStatus() != PayoutStatus.FAILED)
                .map(Payout::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paidOut = payoutRepository.findByCenterIdOrderByRequestedAtDesc(center.getId()).stream()
                .filter(po -> po.getStatus() == PayoutStatus.PAID)
                .map(Payout::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CenterBalancesResponse(
                held, releasedNet.subtract(payoutsTaken), paidOut, gross, commission, net, "KWD");
    }

    // ── Per-booking settlement ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public SettlementResponse getSettlement(User caller, Long bookingId) {
        Booking booking = loadBooking(bookingId);
        centerSecurity.requirePermission(booking.getCenter().getId(), caller, CenterPermission.VIEW_REVENUE);
        Payment p = latestPayment(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not paid yet"));
        return new SettlementResponse(
                bookingId, deriveLines(booking),
                p.getGrossAmount(), p.getCommissionRate(), p.getCommissionAmount(),
                nz(p.getRefundedAmount()), p.getNetAmount(), p.getStatus(),
                p.isReleaseEligible(), p.isDisputed(), p.getAutoReleaseAt());
    }

    // ── Deposit config ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DepositConfigResponse getDepositConfig(User caller) {
        MaintenanceCenter center = resolveMyCenter(caller);
        centerSecurity.requirePermission(center.getId(), caller, CenterPermission.MANAGE_PRICING);
        DepositConfig cfg = depositConfigRepository.findByCenterId(center.getId()).orElse(null);
        if (cfg == null) {
            return new DepositConfigResponse(DepositMode.NONE.name(), null, null, null, CancellationPolicy.RETAIN.name());
        }
        return toDepositResponse(cfg);
    }

    @Transactional
    public DepositConfigResponse updateDepositConfig(User caller, UpdateDepositConfigRequest req) {
        MaintenanceCenter center = resolveMyCenter(caller);
        centerSecurity.requirePermission(center.getId(), caller, CenterPermission.MANAGE_PRICING);
        if (req.mode() == DepositMode.PERCENT && (req.percent() == null || req.percent() < 0 || req.percent() > 100)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Percent must be 0–100");
        }
        if (req.mode() == DepositMode.FLAT && (req.flatAmount() == null || req.flatAmount().signum() < 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Flat amount must be ≥ 0");
        }
        DepositConfig cfg = depositConfigRepository.findByCenterId(center.getId())
                .orElseGet(() -> {
                    DepositConfig c = new DepositConfig();
                    c.setCenter(center);
                    return c;
                });
        cfg.setMode(req.mode());
        cfg.setFlatAmount(req.flatAmount());
        cfg.setPercent(req.percent());
        cfg.setAppliesToServiceId(req.appliesToServiceId());
        cfg.setCancellationPolicy(req.cancellationPolicy() != null ? req.cancellationPolicy() : CancellationPolicy.RETAIN);
        return toDepositResponse(depositConfigRepository.save(cfg));
    }

    // ── Refunds ───────────────────────────────────────────────────────────────

    @Transactional
    public RefundResponse refundBooking(User caller, Long bookingId, RefundRequestDto req) {
        Booking booking = loadBooking(bookingId);
        centerSecurity.requirePermission(booking.getCenter().getId(), caller, CenterPermission.MANAGE_PAYOUTS);
        Payment p = latestPayment(bookingId)
                .filter(x -> CAPTURED.contains(x.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No captured payment to refund"));

        BigDecimal maxRefundable = nz(p.getGrossAmount()).subtract(nz(p.getRefundedAmount()));
        if (req.amount().compareTo(maxRefundable) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refund exceeds the refundable amount");
        }

        // Credit the customer's wallet (v1 refunds land in the wallet).
        Wallet wallet = getOrCreateWallet(p.getCustomer());
        wallet.setBalance(wallet.getBalance().add(req.amount()));
        walletRepository.save(wallet);
        recordWalletTx(wallet, WalletTransactionType.REFUND, req.amount(), bookingId,
                "Refund for booking", "استرداد للحجز");

        p.setRefundedAmount(nz(p.getRefundedAmount()).add(req.amount()));
        if (p.getRefundedAmount().compareTo(nz(p.getGrossAmount())) >= 0) {
            p.setStatus(PaymentStatus.REFUNDED);
        }
        paymentRepository.save(p);
        log.info("Refund {} on booking {} → payment {} refunded {}",
                req.amount(), bookingId, p.getId(), p.getRefundedAmount());
        return new RefundResponse(bookingId, p.getStatus(), p.getRefundedAmount(),
                nz(p.getNetAmount()).subtract(p.getRefundedAmount()));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Booking loadBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingId));
    }

    private java.util.Optional<Payment> latestPayment(Long bookingId) {
        return paymentRepository.findByBookingIdOrderByCreatedAtDesc(bookingId).stream().findFirst();
    }

    private MaintenanceCenter resolveMyCenter(User caller) {
        return centerRepository.findFirstByOwnerId(caller.getId())
                .or(() -> membershipRepository
                        .findByUserIdAndStatus(caller.getId(), MembershipStatus.ACTIVE)
                        .stream().findFirst().map(CenterMembership::getCenter))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "No center associated with this account"));
    }

    private Wallet getOrCreateWallet(User user) {
        return walletRepository.findByUserId(user.getId()).orElseGet(() -> {
            Wallet w = new Wallet();
            w.setUser(user);
            w.setBalance(BigDecimal.ZERO);
            return walletRepository.save(w);
        });
    }

    private void recordWalletTx(Wallet wallet, WalletTransactionType type, BigDecimal amount,
                                Long bookingId, String descEn, String descAr) {
        WalletTransaction tx = new WalletTransaction();
        tx.setWallet(wallet);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setBookingId(bookingId);
        tx.setDescriptionEn(descEn);
        tx.setDescriptionAr(descAr);
        walletTxRepository.save(tx);
    }

    private static DepositConfigResponse toDepositResponse(DepositConfig c) {
        return new DepositConfigResponse(
                c.getMode().name(), c.getFlatAmount(), c.getPercent(),
                c.getAppliesToServiceId(), c.getCancellationPolicy().name());
    }

    private List<InvoiceLineDto> deriveLines(Booking booking) {
        List<BookingQuote> quotes = quoteRepository.findByBookingIdOrderByVersionDesc(booking.getId());
        BookingQuote quote = quotes.stream()
                .filter(q -> q.getStatus() == QuoteStatus.APPROVED)
                .findFirst()
                .orElse(quotes.stream().findFirst().orElse(null));
        List<InvoiceLineDto> lines = new ArrayList<>();
        if (quote != null && quote.getTotalAmount() != null) {
            for (QuoteLineItem li : quote.getLineItems()) {
                BigDecimal amount = nz(li.getPartsCost()).add(nz(li.getLaborCost()));
                String kind = li.getKind() == QuoteLineItemKind.DIAGNOSTIC_FEE ? "DIAGNOSTIC_FEE" : "SERVICE";
                lines.add(new InvoiceLineDto(
                        li.getDescription() != null ? li.getDescription() : "Service",
                        li.getDescriptionAr() != null ? li.getDescriptionAr() : "خدمة", amount, kind));
            }
            if (quote.getDiscountAmount() != null && quote.getDiscountAmount().signum() > 0) {
                lines.add(new InvoiceLineDto("Discount", "خصم", quote.getDiscountAmount().negate(), "DISCOUNT"));
            }
            return lines;
        }
        BigDecimal cost = booking.getFinalCost() != null ? booking.getFinalCost()
                : booking.getEstimatedCost() != null ? booking.getEstimatedCost() : BigDecimal.ZERO;
        lines.add(new InvoiceLineDto("Service", "خدمة", cost, "SERVICE"));
        return lines;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
