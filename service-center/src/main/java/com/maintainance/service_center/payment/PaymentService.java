package com.maintainance.service_center.payment;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.booking.BookingStatus;
import com.maintainance.service_center.quote.BookingQuote;
import com.maintainance.service_center.quote.BookingQuoteRepository;
import com.maintainance.service_center.quote.QuoteLineItem;
import com.maintainance.service_center.quote.QuoteLineItemKind;
import com.maintainance.service_center.quote.QuoteStatus;
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
import java.util.List;

/**
 * Spec 007 — in-app payments, wallet & escrow. The gateway is abstracted (see {@link PaymentGateway});
 * escrow is modelled in our DB as the {@link Payment} status machine, so this works end-to-end with
 * the stub gateway and is unchanged when a real, webhook-driven gateway is wired.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.050");
    private static final long AUTO_RELEASE_HOURS = 72;
    private static final String RETURN_URL_PREFIX = "https://app.maintenance.example/payment-return";
    private static final List<PaymentMethod> AVAILABLE_METHODS =
            List.of(PaymentMethod.KNET, PaymentMethod.CARD, PaymentMethod.APPLE_PAY, PaymentMethod.WALLET);
    private static final java.util.Set<PaymentStatus> CAPTURED =
            java.util.EnumSet.of(PaymentStatus.HELD, PaymentStatus.RELEASED, PaymentStatus.PAID);

    private final PaymentRepository paymentRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTxRepository;
    private final SavedMethodRepository savedMethodRepository;
    private final BookingRepository bookingRepository;
    private final BookingQuoteRepository quoteRepository;
    private final DepositConfigRepository depositConfigRepository;
    private final PaymentGateway gateway;

    // ── Invoice ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public BookingInvoiceResponse getInvoice(User caller, Long bookingId) {
        Booking booking = requireOwnedBooking(caller, bookingId);
        List<InvoiceLineDto> lines = deriveLines(booking);
        BigDecimal total = lines.stream().map(InvoiceLineDto::amount).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Payment> payments = paymentRepository.findByBookingIdOrderByCreatedAtDesc(bookingId);
        // The invoice status reflects the FULL (balance) payment; the deposit is summarised apart.
        Payment fullPayment = payments.stream().filter(p -> kindOf(p) == PaymentKind.FULL).findFirst().orElse(null);
        PaymentStatus status = fullPayment != null ? fullPayment.getStatus() : PaymentStatus.PENDING;
        boolean releaseEligible = fullPayment != null && fullPayment.isReleaseEligible();
        boolean settled = status == PaymentStatus.RELEASED || status == PaymentStatus.PAID;

        BigDecimal depositRequired = nz(booking.getDepositAmount());
        BigDecimal depositPaid = capturedDepositTotal(payments);
        BigDecimal fullPaid = fullPayment != null && CAPTURED.contains(fullPayment.getStatus())
                ? nz(fullPayment.getGrossAmount()) : BigDecimal.ZERO;
        BigDecimal amountDue = total.subtract(depositPaid).subtract(fullPaid).max(BigDecimal.ZERO);
        BigDecimal paidAmount = depositPaid.add(fullPaid);
        // Whether a customer cancellation would refund the deposit (RETAIN ⇒ forfeited). Default
        // REFUND when the center has no policy configured (matches the cancellation disposition).
        boolean depositRefundable = depositConfigRepository.findByCenterId(booking.getCenter().getId())
                .map(c -> c.getCancellationPolicy() != CancellationPolicy.RETAIN)
                .orElse(true);

        return new BookingInvoiceResponse(
                bookingId, lines, total, "KWD", status,
                paidAmount.signum() > 0 ? paidAmount : null,
                true, AVAILABLE_METHODS, releaseEligible,
                fullPayment != null ? fullPayment.getAutoReleaseAt() : null,
                settled ? "https://app.maintenance.example/receipts/" + bookingId : null,
                depositRequired, depositPaid, amountDue, depositRefundable);
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
                        li.getDescriptionAr() != null ? li.getDescriptionAr() : "خدمة",
                        amount, kind));
            }
            if (quote.getDiscountAmount() != null && quote.getDiscountAmount().signum() > 0) {
                lines.add(new InvoiceLineDto("Discount", "خصم", quote.getDiscountAmount().negate(), "DISCOUNT"));
            }
            return withFulfillmentFee(lines, booking);
        }
        // Fallback: a single line from the booking cost.
        BigDecimal cost = booking.getFinalCost() != null ? booking.getFinalCost()
                : booking.getEstimatedCost() != null ? booking.getEstimatedCost() : BigDecimal.ZERO;
        lines.add(new InvoiceLineDto("Service", "خدمة", cost, "SERVICE"));
        return withFulfillmentFee(lines, booking);
    }

    /** Spec 008 — append the fulfillment fee (pickup/at-home) as an invoice line when present. */
    private List<InvoiceLineDto> withFulfillmentFee(List<InvoiceLineDto> lines, Booking booking) {
        if (booking.getFulfillmentFee() != null && booking.getFulfillmentFee().signum() > 0) {
            lines.add(new InvoiceLineDto("Fulfillment fee", "رسوم التنفيذ",
                    booking.getFulfillmentFee(), "FULFILLMENT_FEE"));
        }
        return lines;
    }

    // ── Payment ──────────────────────────────────────────────────────────────

    @Transactional
    public InitiatePaymentResponseDto initiate(User caller, InitiatePaymentRequestDto req) {
        // Idempotent: reuse the existing attempt for the same key (FR-011).
        Payment existing = paymentRepository.findByIdempotencyKey(req.idempotencyKey()).orElse(null);
        if (existing != null) {
            return toInitiateResponse(existing, existing.getGatewayReference() != null
                    ? "https://gateway.stub.local/checkout/" + existing.getGatewayReference() : null);
        }

        Booking booking = requireOwnedBooking(caller, req.bookingId());
        List<Payment> payments = paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId());
        BigDecimal total = deriveLines(booking).stream()
                .map(InvoiceLineDto::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nothing to pay for this booking");
        }
        // The balance owed nets out any deposit already captured (spec 023).
        BigDecimal amountDue = total.subtract(capturedDepositTotal(payments));
        if (amountDue.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This booking is already covered by the deposit");
        }
        return capturePayment(caller, booking, amountDue, req, PaymentKind.FULL);
    }

    /**
     * Spec 023 — collect the booking's required deposit upfront (escrow-held like any payment, and
     * later credited against the balance). Captures {@code booking.depositAmount}.
     */
    @Transactional
    public InitiatePaymentResponseDto initiateDeposit(User caller, InitiatePaymentRequestDto req) {
        Payment existing = paymentRepository.findByIdempotencyKey(req.idempotencyKey()).orElse(null);
        if (existing != null) {
            return toInitiateResponse(existing, existing.getGatewayReference() != null
                    ? "https://gateway.stub.local/checkout/" + existing.getGatewayReference() : null);
        }
        Booking booking = requireOwnedBooking(caller, req.bookingId());
        BigDecimal deposit = nz(booking.getDepositAmount());
        if (deposit.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No deposit is required for this booking");
        }
        List<Payment> payments = paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId());
        if (capturedDepositTotal(payments).signum() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Deposit already paid");
        }
        return capturePayment(caller, booking, deposit, req, PaymentKind.DEPOSIT);
    }

    /** Shared capture flow: commission snapshot, wallet-first split, gateway/stub capture. */
    private InitiatePaymentResponseDto capturePayment(
            User caller, Booking booking, BigDecimal gross, InitiatePaymentRequestDto req, PaymentKind kind) {
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setCustomer(caller);
        payment.setCenter(booking.getCenter());
        payment.setKind(kind);
        payment.setGrossAmount(gross);
        payment.setCommissionRate(COMMISSION_RATE);
        BigDecimal commission = gross.multiply(COMMISSION_RATE).setScale(3, RoundingMode.HALF_UP);
        payment.setCommissionAmount(commission);
        payment.setNetAmount(gross.subtract(commission));
        payment.setMethod(req.method());
        payment.setIdempotencyKey(req.idempotencyKey());
        payment.setStatus(PaymentStatus.PENDING);

        boolean deposit = kind == PaymentKind.DEPOSIT;
        // Wallet-first split (R6).
        BigDecimal walletApplied = BigDecimal.ZERO;
        if (req.useWalletBalance()) {
            Wallet wallet = getOrCreateWallet(caller);
            walletApplied = wallet.getBalance().min(gross);
            if (walletApplied.signum() > 0) {
                wallet.setBalance(wallet.getBalance().subtract(walletApplied));
                walletRepository.save(wallet);
                recordWalletTx(wallet, WalletTransactionType.PAYMENT, walletApplied.negate(),
                        booking.getId(), deposit ? "Deposit for booking" : "Payment for booking",
                        deposit ? "عربون للحجز" : "دفعة للحجز");
            }
        }
        payment.setWalletAmount(walletApplied);
        BigDecimal externalAmount = gross.subtract(walletApplied);

        payment = paymentRepository.save(payment); // get id before talking to the gateway

        String checkoutUrl = null;
        if (externalAmount.signum() <= 0) {
            capture(payment); // wallet covered the whole amount — no gateway step
        } else {
            PaymentGateway.CheckoutSession session = gateway.createCheckout(payment, externalAmount);
            payment.setGatewayReference(session.reference());
            checkoutUrl = session.checkoutUrl();
            if (gateway.capturesSynchronously()) {
                capture(payment); // stub: capture now (a real gateway confirms via webhook)
            }
        }

        if (Boolean.TRUE.equals(req.saveCard()) && req.method() == PaymentMethod.CARD
                && payment.getStatus() == PaymentStatus.HELD) {
            saveStubCard(caller);
        }
        paymentRepository.save(payment);
        log.info("Payment {} ({}) for booking {} → {} (wallet {}, external {})",
                payment.getId(), kind, booking.getId(), payment.getStatus(), walletApplied, externalAmount);
        return toInitiateResponse(payment, checkoutUrl);
    }

    private void capture(Payment payment) {
        payment.setStatus(PaymentStatus.HELD);
        payment.setCapturedAt(LocalDateTime.now());
        payment.setAutoReleaseAt(LocalDateTime.now().plusHours(AUTO_RELEASE_HOURS));
    }

    /**
     * Hosted-checkout callback (the gateway's webhook/redirect): confirm or fail a PENDING payment by
     * its gateway reference. Idempotent — a repeat callback after settlement is a no-op. This is the
     * single point a real provider's webhook would call; the mock hosted page calls it too.
     */
    @Transactional
    public PaymentStatus confirmGatewayCallback(String gatewayReference, boolean success) {
        Payment p = paymentRepository.findByGatewayReference(gatewayReference)
                .orElseThrow(() -> new EntityNotFoundException("Unknown gateway reference: " + gatewayReference));
        if (p.getStatus() != PaymentStatus.PENDING) {
            return p.getStatus(); // already captured/failed — idempotent
        }
        if (success) {
            capture(p);
            log.info("Gateway callback: payment {} ({}) captured → HELD", p.getId(), gatewayReference);
        } else {
            p.setStatus(PaymentStatus.FAILED);
            log.info("Gateway callback: payment {} ({}) failed", p.getId(), gatewayReference);
        }
        paymentRepository.save(p);
        return p.getStatus();
    }

    /** External (non-wallet) amount still owed on a payment — what the hosted checkout collects. */
    BigDecimal externalAmountOf(Payment p) {
        return nz(p.getGrossAmount()).subtract(nz(p.getWalletAmount()));
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponseDto getStatus(User caller, Long paymentId) {
        Payment p = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found: " + paymentId));
        if (!p.getCustomer().getId().equals(caller.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your payment");
        }
        return new PaymentStatusResponseDto(p.getId(), p.getStatus(), p.getBooking().getId());
    }

    @Transactional
    public ReleaseResponseDto release(User caller, Long bookingId) {
        requireOwnedBooking(caller, bookingId);
        // Release every held payment for the booking together — deposit + balance (spec 023).
        List<Payment> held = paymentRepository.findByBookingIdOrderByCreatedAtDesc(bookingId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.HELD)
                .toList();
        if (held.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No held payment to act on");
        }
        for (Payment p : held) {
            if (p.isDisputed()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment is under dispute");
            }
            if (!p.isReleaseEligible()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Work is not marked complete yet");
            }
        }
        LocalDateTime now = LocalDateTime.now();
        for (Payment p : held) {
            p.setStatus(PaymentStatus.RELEASED);
            p.setReleasedAt(now);
        }
        paymentRepository.saveAll(held);
        return new ReleaseResponseDto(bookingId, PaymentStatus.RELEASED);
    }

    @Transactional
    public DisputeResponseDto dispute(User caller, Long bookingId, String reason) {
        requireOwnedBooking(caller, bookingId);
        Payment held = latestHeld(bookingId);
        held.setDisputed(true);
        held.setDisputeReason(reason);
        paymentRepository.save(held);
        return new DisputeResponseDto(bookingId, held.getStatus(), true);
    }

    // ── Wallet ───────────────────────────────────────────────────────────────

    @Transactional
    public WalletResponseDto getWallet(User caller) {
        Wallet wallet = getOrCreateWallet(caller);
        return new WalletResponseDto(wallet.getBalance(), "KWD");
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponseDto> getWalletTransactions(User caller) {
        Wallet wallet = walletRepository.findByUserId(caller.getId()).orElse(null);
        if (wallet == null) return List.of();
        return walletTxRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId()).stream()
                .map(tx -> new WalletTransactionResponseDto(
                        tx.getId(), tx.getType().name(), tx.getAmount(), tx.getBookingId(),
                        tx.getCreatedAt(), tx.getDescriptionEn(), tx.getDescriptionAr()))
                .toList();
    }

    @Transactional
    public InitiatePaymentResponseDto topUp(User caller, TopUpRequestDto req) {
        Wallet wallet = getOrCreateWallet(caller);
        // Stub gateway captures synchronously: credit the wallet now. A real gateway returns a
        // checkoutUrl and credits via the webhook; the client then opens the hosted page.
        if (gateway.capturesSynchronously()) {
            wallet.setBalance(wallet.getBalance().add(req.amount()));
            walletRepository.save(wallet);
            recordWalletTx(wallet, WalletTransactionType.TOPUP, req.amount(), null, "Top-up", "شحن");
            return new InitiatePaymentResponseDto(0L, PaymentStatus.PAID, null, RETURN_URL_PREFIX);
        }
        // Real gateway path: hand back a checkout URL (capture confirmed by webhook).
        return new InitiatePaymentResponseDto(0L, PaymentStatus.PENDING,
                "https://gateway.stub.local/topup", RETURN_URL_PREFIX);
    }

    // ── Saved methods ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SavedMethodResponseDto> listSavedMethods(User caller) {
        return savedMethodRepository.findByCustomerId(caller.getId()).stream()
                .map(m -> new SavedMethodResponseDto(m.getId(), m.getBrand(), m.getMaskedLabel(), m.getExpiry()))
                .toList();
    }

    @Transactional
    public void deleteSavedMethod(User caller, Long id) {
        SavedMethod m = savedMethodRepository.findByIdAndCustomerId(id, caller.getId())
                .orElseThrow(() -> new EntityNotFoundException("Saved method not found"));
        savedMethodRepository.delete(m);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Booking requireOwnedBooking(User caller, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingId));
        if (booking.getCustomer() == null || !booking.getCustomer().getId().equals(caller.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your booking");
        }
        return booking;
    }

    private Payment latestHeld(Long bookingId) {
        return paymentRepository.findByBookingIdOrderByCreatedAtDesc(bookingId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.HELD)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No held payment to act on"));
    }

    private Wallet getOrCreateWallet(User caller) {
        return walletRepository.findByUserId(caller.getId()).orElseGet(() -> {
            Wallet w = new Wallet();
            w.setUser(caller);
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

    private void saveStubCard(User caller) {
        SavedMethod m = new SavedMethod();
        m.setCustomer(caller);
        m.setBrand("visa");
        m.setMaskedLabel("•••• 4242");
        m.setExpiry("08/27");
        m.setGatewayToken("tok_stub_" + caller.getId());
        savedMethodRepository.save(m);
    }

    private InitiatePaymentResponseDto toInitiateResponse(Payment p, String checkoutUrl) {
        return new InitiatePaymentResponseDto(p.getId(), p.getStatus(), checkoutUrl, RETURN_URL_PREFIX);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    /** Legacy rows persisted before the deposit feature have a null kind — treat them as FULL. */
    private static PaymentKind kindOf(Payment p) {
        return p.getKind() != null ? p.getKind() : PaymentKind.FULL;
    }

    /** Sum of captured DEPOSIT payments for a booking — what the balance owed is netted against. */
    private BigDecimal capturedDepositTotal(List<Payment> bookingPayments) {
        return bookingPayments.stream()
                .filter(p -> kindOf(p) == PaymentKind.DEPOSIT)
                .filter(p -> CAPTURED.contains(p.getStatus()))
                .map(p -> nz(p.getGrossAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
