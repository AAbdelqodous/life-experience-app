package com.maintainance.service_center.payment;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.booking.BookingStatus;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.quote.BookingQuote;
import com.maintainance.service_center.quote.BookingQuoteRepository;
import com.maintainance.service_center.quote.QuoteLineItem;
import com.maintainance.service_center.quote.QuoteStatus;
import com.maintainance.service_center.role.Role;
import com.maintainance.service_center.role.RoleRepository;
import com.maintainance.service_center.staff.CenterMembership;
import com.maintainance.service_center.staff.CenterMembershipRepository;
import com.maintainance.service_center.staff.CenterRole;
import com.maintainance.service_center.staff.MembershipStatus;
import com.maintainance.service_center.user.User;
import com.maintainance.service_center.user.UserRepository;
import com.maintainance.service_center.user.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import com.maintainance.service_center.booking.FulfillmentMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Spec 007 (customer) + 023 (center) — end-to-end payment/escrow against the real DB:
 * invoice → initiate (stub gateway captures → HELD) → center marks complete → customer releases
 * → RELEASED; the center's earnings + settlement reflect it. Plus wallet top-up / wallet-first
 * split and idempotent initiation. Requires a running Postgres.
 */
@SpringBootTest
@ActiveProfiles("dev")
class PaymentIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired MaintenanceCenterRepository centerRepository;
    @Autowired CenterMembershipRepository membershipRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired BookingQuoteRepository quoteRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired WalletRepository walletRepository;
    @Autowired PaymentService service;
    @Autowired CenterPaymentService centerService;
    @Autowired CenterPayoutService payoutService;
    @Autowired TransactionTemplate tx;

    private User owner;
    private User customer;
    private MaintenanceCenter center;

    @BeforeEach
    void setUp() {
        tx.executeWithoutResult(s -> {
            String suffix = UUID.randomUUID().toString().substring(0, 8);
            Role ownerRole = roleRepository.findByName("ROLE_OWNER")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_OWNER").build()));
            owner = userRepository.save(User.builder()
                    .firstname("Pay").lastname("Owner").email("pay-owner-" + suffix + "@test.local")
                    .password(passwordEncoder.encode("Test@1234")).enabled(true).accountLocked(false)
                    .userType(UserType.OWNER).roles(List.of(ownerRole)).build());
            customer = userRepository.save(User.builder()
                    .firstname("Pay").lastname("Cust").email("pay-cust-" + suffix + "@test.local")
                    .password(passwordEncoder.encode("Test@1234")).enabled(true).accountLocked(false)
                    .userType(UserType.CUSTOMER).build());
            center = centerRepository.save(MaintenanceCenter.builder()
                    .nameAr("مركز " + suffix).nameEn("Pay Center " + suffix)
                    .email("pay-c-" + suffix + "@test.local").phone("+96555444" + suffix.substring(0, 3))
                    .categories(new ArrayList<>()).workingDays(new ArrayList<>())
                    .specializations(new ArrayList<>()).imageUrls(new ArrayList<>())
                    .certifications(new ArrayList<>()).owner(owner)
                    .isVerified(false).isActive(true).enabled(true).build());
            membershipRepository.save(CenterMembership.builder()
                    .user(owner).center(center)
                    .role(CenterRole.OWNER).status(MembershipStatus.ACTIVE)
                    .activatedAt(LocalDateTime.now()).build());
        });
    }

    private Booking bookingWithApprovedQuote(String total) {
        return tx.execute(s -> {
            Booking b = bookingRepository.save(Booking.builder()
                    .bookingNumber("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .customer(customer).center(center)
                    .bookingDate(LocalDate.now()).bookingTime(LocalTime.of(10, 0))
                    .bookingStatus(BookingStatus.IN_PROGRESS).build());
            QuoteLineItem li = new QuoteLineItem();
            li.setDescription("AC repair");
            li.setDescriptionAr("إصلاح مكيف");
            li.setPartsCost(new BigDecimal("18.000"));
            li.setLaborCost(new BigDecimal("10.500"));
            BookingQuote q = new BookingQuote();
            q.setBooking(b);
            q.setLineItems(List.of(li));
            q.setSubtotal(new BigDecimal(total));
            q.setTotalAmount(new BigDecimal(total));
            q.setStatus(QuoteStatus.APPROVED);
            quoteRepository.save(q);
            return b;
        });
    }

    @Test
    void fullFlow_pay_markComplete_release_earnings_settlement() {
        Booking b = bookingWithApprovedQuote("28.500");

        BookingInvoiceResponse inv = service.getInvoice(customer, b.getId());
        assertThat(inv.total()).isEqualByComparingTo("28.500");
        assertThat(inv.releaseEligible()).isFalse(); // not paid / not completed yet

        var init = service.initiate(customer, new InitiatePaymentRequestDto(
                b.getId(), PaymentMethod.KNET, false, false, null, "pay-" + b.getId()));
        assertThat(init.status()).isEqualTo(PaymentStatus.HELD);
        assertThat(init.checkoutUrl()).isNotNull();

        // Held but not yet release-eligible.
        assertThat(service.getInvoice(customer, b.getId()).releaseEligible()).isFalse();

        // Completing the booking folds in the escrow release-trigger → customer release eligible.
        var mc = centerService.markReleaseEligible(b.getId());
        assertThat(mc.releaseEligible()).isTrue();
        assertThat(service.getInvoice(customer, b.getId()).releaseEligible()).isTrue();

        var released = service.release(customer, b.getId());
        assertThat(released.paymentStatus()).isEqualTo(PaymentStatus.RELEASED);

        // Center earnings: the net (28.500 − 1.425 commission = 27.075) is now Available, not Held.
        CenterBalancesResponse earnings = centerService.getEarnings(owner);
        assertThat(earnings.available()).isEqualByComparingTo("27.075");
        assertThat(earnings.held()).isEqualByComparingTo("0.000");

        SettlementResponse settlement = centerService.getSettlement(owner, b.getId());
        assertThat(settlement.commissionAmount()).isEqualByComparingTo("1.425");
        assertThat(settlement.net()).isEqualByComparingTo("27.075");
        assertThat(settlement.paymentStatus()).isEqualTo(PaymentStatus.RELEASED);
    }

    @Test
    void walletTopUp_thenWalletCoversFullPayment() {
        service.topUp(customer, new TopUpRequestDto(new BigDecimal("100.000"), PaymentMethod.KNET, "top-" + UUID.randomUUID()));
        assertThat(service.getWallet(customer).balance()).isEqualByComparingTo("100.000");

        Booking b = bookingWithApprovedQuote("28.500");
        var init = service.initiate(customer, new InitiatePaymentRequestDto(
                b.getId(), PaymentMethod.WALLET, true, false, null, "wpay-" + b.getId()));

        assertThat(init.status()).isEqualTo(PaymentStatus.HELD);
        assertThat(init.checkoutUrl()).isNull(); // wallet covered the whole amount
        assertThat(service.getWallet(customer).balance()).isEqualByComparingTo("71.500");
    }

    @Test
    void initiate_isIdempotent() {
        Booking b = bookingWithApprovedQuote("28.500");
        String key = "idem-" + b.getId();
        var first = service.initiate(customer, new InitiatePaymentRequestDto(
                b.getId(), PaymentMethod.KNET, false, false, null, key));
        var second = service.initiate(customer, new InitiatePaymentRequestDto(
                b.getId(), PaymentMethod.KNET, false, false, null, key));
        assertThat(second.paymentId()).isEqualTo(first.paymentId());
        assertThat(paymentRepository.findByBookingIdOrderByCreatedAtDesc(b.getId())).hasSize(1);
    }

    @Test
    void refund_creditsCustomerWallet_andReducesSettlementNet() {
        Booking b = bookingWithApprovedQuote("28.500");
        service.initiate(customer, new InitiatePaymentRequestDto(
                b.getId(), PaymentMethod.KNET, false, false, null, "rf-" + b.getId()));
        BigDecimal before = service.getWallet(customer).balance();

        var res = centerService.refundBooking(owner, b.getId(), new RefundRequestDto(new BigDecimal("10.000"), "WALLET", "overcharge"));
        assertThat(res.refundedAmount()).isEqualByComparingTo("10.000");
        assertThat(service.getWallet(customer).balance()).isEqualByComparingTo(before.add(new BigDecimal("10.000")));
        assertThat(centerService.getSettlement(owner, b.getId()).refundedAmount()).isEqualByComparingTo("10.000");
    }

    @Test
    void depositConfig_roundTrips() {
        var saved = centerService.updateDepositConfig(owner,
                new UpdateDepositConfigRequest(DepositMode.PERCENT, null, 20, null, CancellationPolicy.RETAIN));
        assertThat(saved.mode()).isEqualTo("PERCENT");
        assertThat(saved.percent()).isEqualTo(20);
        assertThat(centerService.getDepositConfig(owner).percent()).isEqualTo(20);
    }

    @Test
    void payout_requestedFromAvailableBalance() {
        // Settle a payment so the center has Available balance (net 27.075).
        Booking b = bookingWithApprovedQuote("28.500");
        service.initiate(customer, new InitiatePaymentRequestDto(
                b.getId(), PaymentMethod.KNET, false, false, null, "po-" + b.getId()));
        centerService.markReleaseEligible(b.getId());
        service.release(customer, b.getId());

        var account = payoutService.upsertAccount(owner,
                new UpsertPayoutAccountRequest("KW81CBKU0000000000001234560101", "Pay Center Co"));
        assertThat(account.status()).isEqualTo("VERIFIED");

        var payout = payoutService.requestPayout(owner, new RequestPayoutRequest(new BigDecimal("25.000"), account.id()));
        assertThat(payout.status()).isEqualTo("REQUESTED");
        assertThat(payout.reference()).startsWith("PO-");

        // Available drops by the requested payout: 27.075 − 25.000 = 2.075.
        assertThat(centerService.getEarnings(owner).available()).isEqualByComparingTo("2.075");
        assertThat(payoutService.listPayouts(owner)).hasSize(1);
    }

    // ── Spec 023: deposit applied at booking creation ──

    private Long bareBooking(String estimatedCost) {
        return tx.execute(s -> bookingRepository.save(Booking.builder()
                .bookingNumber("DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .customer(customer).center(center)
                .bookingDate(LocalDate.now()).bookingTime(LocalTime.of(10, 0))
                .bookingStatus(BookingStatus.PENDING)
                .estimatedCost(estimatedCost == null ? null : new BigDecimal(estimatedCost))
                .build()).getId());
    }

    @Test
    void deposit_flat_appliedOnCreation() {
        centerService.updateDepositConfig(owner,
                new UpdateDepositConfigRequest(DepositMode.FLAT, new BigDecimal("5.000"), null, null, CancellationPolicy.RETAIN));
        Long id = bareBooking(null);
        centerService.applyDepositOnCreation(id);
        assertThat(bookingRepository.findById(id).orElseThrow().getDepositAmount()).isEqualByComparingTo("5.000");
    }

    @Test
    void deposit_percent_appliedOnCreationFromEstimate() {
        centerService.updateDepositConfig(owner,
                new UpdateDepositConfigRequest(DepositMode.PERCENT, null, 20, null, CancellationPolicy.RETAIN));
        Long id = bareBooking("30.000"); // 20% of 30.000 = 6.000
        centerService.applyDepositOnCreation(id);
        assertThat(bookingRepository.findById(id).orElseThrow().getDepositAmount()).isEqualByComparingTo("6.000");
    }

    @Test
    void deposit_percent_skippedWhenNoEstimate() {
        centerService.updateDepositConfig(owner,
                new UpdateDepositConfigRequest(DepositMode.PERCENT, null, 20, null, CancellationPolicy.RETAIN));
        Long id = bareBooking(null); // no estimate → deferred to quote time
        centerService.applyDepositOnCreation(id);
        assertThat(bookingRepository.findById(id).orElseThrow().getDepositAmount()).isNull();
    }

    @Test
    void deposit_none_leavesBookingUntouched() {
        // No DepositConfig saved for this center → no deposit.
        Long id = bareBooking("30.000");
        centerService.applyDepositOnCreation(id);
        assertThat(bookingRepository.findById(id).orElseThrow().getDepositAmount()).isNull();
    }

    @Test
    void deposit_paidUpfront_creditsBalance_andSettlesTogether() {
        centerService.updateDepositConfig(owner,
                new UpdateDepositConfigRequest(DepositMode.FLAT, new BigDecimal("5.000"), null, null, CancellationPolicy.RETAIN));
        Booking b = bookingWithApprovedQuote("28.500");
        centerService.applyDepositOnCreation(b.getId());
        assertThat(bookingRepository.findById(b.getId()).orElseThrow().getDepositAmount()).isEqualByComparingTo("5.000");

        // 1) Pay the deposit upfront → held in escrow.
        service.initiateDeposit(customer, new InitiatePaymentRequestDto(
                b.getId(), PaymentMethod.KNET, false, false, null, "dep-" + b.getId()));
        var inv = service.getInvoice(customer, b.getId());
        assertThat(inv.total()).isEqualByComparingTo("28.500");
        assertThat(inv.depositPaid()).isEqualByComparingTo("5.000");
        assertThat(inv.amountDue()).isEqualByComparingTo("23.500");
        assertThat(inv.paymentStatus()).isEqualTo(PaymentStatus.PENDING); // balance still owed

        // 2) Pay the balance → invoice fully covered, escrow held.
        service.initiate(customer, new InitiatePaymentRequestDto(
                b.getId(), PaymentMethod.KNET, false, false, null, "bal-" + b.getId()));
        var inv2 = service.getInvoice(customer, b.getId());
        assertThat(inv2.amountDue()).isEqualByComparingTo("0.000");
        assertThat(inv2.paymentStatus()).isEqualTo(PaymentStatus.HELD);

        // 3) Complete → both held payments release-eligible → release both.
        centerService.markReleaseEligible(b.getId());
        service.release(customer, b.getId());

        // 4) Settlement aggregates deposit + balance: 28.500 gross − 1.425 commission = 27.075 net.
        var settlement = centerService.getSettlement(owner, b.getId());
        assertThat(settlement.gross()).isEqualByComparingTo("28.500");
        assertThat(settlement.commissionAmount()).isEqualByComparingTo("1.425");
        assertThat(settlement.net()).isEqualByComparingTo("27.075");
        assertThat(settlement.paymentStatus()).isEqualTo(PaymentStatus.RELEASED);
        assertThat(centerService.getEarnings(owner).available()).isEqualByComparingTo("27.075");
    }

    @Test
    void initiateDeposit_rejectedWhenNoDepositRequired() {
        Booking b = bookingWithApprovedQuote("28.500"); // no deposit config → no deposit on the booking
        assertThatThrownBy(() -> service.initiateDeposit(customer, new InitiatePaymentRequestDto(
                b.getId(), PaymentMethod.KNET, false, false, null, "dep-x-" + b.getId())))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No deposit");
    }

    // ── Spec 023: deposit forfeiture / refund on cancellation ──

    private Booking bookingWithPaidDeposit(CancellationPolicy policy, String key) {
        centerService.updateDepositConfig(owner,
                new UpdateDepositConfigRequest(DepositMode.FLAT, new BigDecimal("5.000"), null, null, policy));
        Booking b = bookingWithApprovedQuote("28.500");
        centerService.applyDepositOnCreation(b.getId());
        service.initiateDeposit(customer, new InitiatePaymentRequestDto(
                b.getId(), PaymentMethod.KNET, false, false, null, key + b.getId()));
        return b;
    }

    @Test
    void deposit_forfeitedToCenter_whenCustomerCancels_underRetain() {
        Booking b = bookingWithPaidDeposit(CancellationPolicy.RETAIN, "dep-ret-");
        BigDecimal walletBefore = service.getWallet(customer).balance();

        centerService.handleDepositOnCancellation(b.getId(), true); // customer cancels

        // Deposit released to the center; customer NOT credited.
        assertThat(service.getWallet(customer).balance()).isEqualByComparingTo(walletBefore);
        assertThat(centerService.getSettlement(owner, b.getId()).paymentStatus()).isEqualTo(PaymentStatus.RELEASED);
        // Center keeps the net of the forfeited deposit: 5.000 − 5% = 4.750.
        assertThat(centerService.getEarnings(owner).available()).isEqualByComparingTo("4.750");
    }

    @Test
    void deposit_refundedToCustomer_whenCustomerCancels_underRefundPolicy() {
        Booking b = bookingWithPaidDeposit(CancellationPolicy.REFUND, "dep-ref-");
        BigDecimal walletBefore = service.getWallet(customer).balance();

        centerService.handleDepositOnCancellation(b.getId(), true);

        assertThat(service.getWallet(customer).balance()).isEqualByComparingTo(walletBefore.add(new BigDecimal("5.000")));
        assertThat(centerService.getEarnings(owner).available()).isEqualByComparingTo("0.000");
    }

    @Test
    void invoice_includesFulfillmentFeeLine() {
        // Spec 008 — a booking with a fulfillment fee surfaces a FULFILLMENT_FEE line in the invoice.
        Booking b = bookingWithApprovedQuote("28.500");
        tx.executeWithoutResult(s -> {
            Booking fresh = bookingRepository.findById(b.getId()).orElseThrow();
            fresh.setFulfillmentMode(FulfillmentMode.AT_HOME);
            fresh.setFulfillmentFee(new BigDecimal("5.000"));
            bookingRepository.save(fresh);
        });
        var invoice = service.getInvoice(customer, b.getId());
        assertThat(invoice.lines()).anyMatch(l -> "FULFILLMENT_FEE".equals(l.kind()));
        assertThat(invoice.total()).isEqualByComparingTo("33.500"); // 28.500 service + 5.000 fulfillment
    }

    @Test
    void deposit_refundedToCustomer_whenCenterCancels_evenUnderRetain() {
        Booking b = bookingWithPaidDeposit(CancellationPolicy.RETAIN, "dep-cc-");
        BigDecimal walletBefore = service.getWallet(customer).balance();

        centerService.handleDepositOnCancellation(b.getId(), false); // center cancels — don't penalise customer

        assertThat(service.getWallet(customer).balance()).isEqualByComparingTo(walletBefore.add(new BigDecimal("5.000")));
        assertThat(centerService.getEarnings(owner).available()).isEqualByComparingTo("0.000");
    }
}
