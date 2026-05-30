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
}
