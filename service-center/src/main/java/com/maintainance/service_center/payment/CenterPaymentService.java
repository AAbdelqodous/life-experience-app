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
        // Flag every held payment for the booking — deposit + balance both become releasable (023).
        List<Payment> held = paymentRepository.findByBookingIdOrderByCreatedAtDesc(bookingId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.HELD)
                .toList();
        if (held.isEmpty()) {
            // No held escrow (e.g. cash booking, or already settled) — completion still succeeds.
            return new MarkCompleteResponse(bookingId, true, null);
        }
        LocalDateTime autoReleaseAt = null;
        for (Payment p : held) {
            p.setReleaseEligible(true);
            if (p.getAutoReleaseAt() == null) {
                p.setAutoReleaseAt(LocalDateTime.now().plusHours(AUTO_RELEASE_HOURS));
            }
            autoReleaseAt = p.getAutoReleaseAt();
        }
        paymentRepository.saveAll(held);
        log.info("Booking {} completed → {} held payment(s) release-eligible (auto-release at {})",
                bookingId, held.size(), autoReleaseAt);
        return new MarkCompleteResponse(bookingId, true, autoReleaseAt);
    }

    // ── Deposit application (spec 023: snapshot the center's deposit at booking creation) ──

    /**
     * Applies the center's deposit policy to a freshly created booking, snapshotting the required
     * amount onto {@code booking.depositAmount}. Triggered by {@link BookingDepositListener} on the
     * BookingCreatedEvent, inside the creation transaction. No-op when the center requires no deposit
     * (or, for PERCENT, when there is no estimate yet — that is deferred to quote time).
     */
    @Transactional
    public void applyDepositOnCreation(Long bookingId) {
        Booking booking = loadBooking(bookingId);
        DepositConfig cfg = depositConfigRepository.findByCenterId(booking.getCenter().getId()).orElse(null);
        BigDecimal deposit = computeDeposit(cfg, booking);
        if (deposit != null && deposit.signum() > 0) {
            booking.setDepositAmount(deposit);
            bookingRepository.save(booking);
            log.info("Booking {} requires a {} KD deposit (mode {})", bookingId, deposit, cfg.getMode());
        }
    }

    /** Resolves the deposit amount for a booking from the center's policy; null when none applies. */
    private BigDecimal computeDeposit(DepositConfig cfg, Booking booking) {
        if (cfg == null || cfg.getMode() == DepositMode.NONE) {
            return null;
        }
        // Service-scoped policy: applies only to the configured service (null = center-wide).
        if (cfg.getAppliesToServiceId() != null) {
            Long bookingServiceId = booking.getService() != null ? booking.getService().getId() : null;
            if (!cfg.getAppliesToServiceId().equals(bookingServiceId)) {
                return null;
            }
        }
        if (cfg.getMode() == DepositMode.FLAT) {
            return cfg.getFlatAmount();
        }
        // PERCENT — needs an estimate to size against; deferred to quote time if absent at creation.
        BigDecimal base = booking.getEstimatedCost();
        if (base == null || cfg.getPercent() == null) {
            return null;
        }
        return base
                .multiply(BigDecimal.valueOf(cfg.getPercent()))
                .divide(BigDecimal.valueOf(100), 3, RoundingMode.HALF_UP);
    }

    // ── Cancellation disposition (spec 023: refund balance; forfeit/refund deposit) ──

    /**
     * Disposes of a cancelled booking's captured funds. The balance (FULL) is always refunded — the
     * work was not done. The deposit is forfeited to the center only when the <em>customer</em>
     * cancelled under a {@link CancellationPolicy#RETAIN} policy (the no-show lever); in every other
     * case (policy REFUND, or the center cancelled) the deposit is refunded too. Idempotent.
     */
    @Transactional
    public void handleDepositOnCancellation(Long bookingId, boolean cancelledByCustomer) {
        Booking booking = loadBooking(bookingId);
        List<Payment> captured = paymentRepository.findByBookingIdOrderByCreatedAtDesc(bookingId).stream()
                .filter(p -> CAPTURED.contains(p.getStatus()))
                .toList();
        if (captured.isEmpty()) {
            return; // nothing captured (e.g. cash, or cancelled before paying) — nothing to dispose
        }
        CancellationPolicy policy = depositConfigRepository.findByCenterId(booking.getCenter().getId())
                .map(DepositConfig::getCancellationPolicy)
                .orElse(CancellationPolicy.REFUND);

        for (Payment p : captured) {
            boolean isDeposit = p.getKind() == PaymentKind.DEPOSIT;
            boolean forfeit = isDeposit && cancelledByCustomer && policy == CancellationPolicy.RETAIN;
            if (forfeit) {
                forfeitToCenter(p);
            } else {
                refundRemainingToCustomer(p, bookingId);
            }
        }
    }

    /** RETAIN no-show: release the held deposit to the center (it settles like a completed job). */
    private void forfeitToCenter(Payment deposit) {
        if (deposit.getStatus() != PaymentStatus.HELD) {
            return; // already released/refunded
        }
        deposit.setReleaseEligible(true);
        deposit.setStatus(PaymentStatus.RELEASED);
        deposit.setReleasedAt(LocalDateTime.now());
        paymentRepository.save(deposit);
        log.info("Deposit payment {} forfeited to center on customer cancellation", deposit.getId());
    }

    /** Refund the not-yet-refunded remainder of a payment to the customer's wallet (v1). */
    private void refundRemainingToCustomer(Payment p, Long bookingId) {
        BigDecimal remaining = nz(p.getGrossAmount()).subtract(nz(p.getRefundedAmount()));
        if (remaining.signum() <= 0) {
            return;
        }
        Wallet wallet = getOrCreateWallet(p.getCustomer());
        wallet.setBalance(wallet.getBalance().add(remaining));
        walletRepository.save(wallet);
        recordWalletTx(wallet, WalletTransactionType.REFUND, remaining, bookingId,
                "Refund for cancelled booking", "استرداد لحجز ملغى");
        p.setRefundedAmount(nz(p.getRefundedAmount()).add(remaining));
        p.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(p);
        log.info("Payment {} ({}) refunded {} to customer on cancellation", p.getId(), p.getKind(), remaining);
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
        // Aggregate across the booking's captured payments — deposit + balance settle together (023).
        List<Payment> captured = paymentRepository.findByBookingIdOrderByCreatedAtDesc(bookingId).stream()
                .filter(p -> CAPTURED.contains(p.getStatus()))
                .toList();
        if (captured.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not paid yet");
        }
        BigDecimal gross = BigDecimal.ZERO, commission = BigDecimal.ZERO;
        BigDecimal refunded = BigDecimal.ZERO, net = BigDecimal.ZERO;
        boolean anyHeld = false, allHeldEligible = true, anyDisputed = false;
        LocalDateTime autoReleaseAt = null;
        for (Payment p : captured) {
            gross = gross.add(nz(p.getGrossAmount()));
            commission = commission.add(nz(p.getCommissionAmount()));
            refunded = refunded.add(nz(p.getRefundedAmount()));
            net = net.add(nz(p.getNetAmount()));
            if (p.getStatus() == PaymentStatus.HELD) {
                anyHeld = true;
                if (!p.isReleaseEligible()) allHeldEligible = false;
                if (p.getAutoReleaseAt() != null) autoReleaseAt = p.getAutoReleaseAt();
            }
            if (p.isDisputed()) anyDisputed = true;
        }
        // Overall status: still escrowed while anything is held; otherwise the settled state.
        PaymentStatus status = anyHeld ? PaymentStatus.HELD : captured.get(0).getStatus();
        boolean releaseEligible = anyHeld ? allHeldEligible : true;
        return new SettlementResponse(
                bookingId, deriveLines(booking),
                gross, captured.get(0).getCommissionRate(), commission,
                refunded, net, status, releaseEligible, anyDisputed, autoReleaseAt);
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
