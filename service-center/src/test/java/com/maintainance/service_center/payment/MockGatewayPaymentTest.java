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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spec 007 — the realistic, MyFatoorah-shaped hosted-checkout path (payment.gateway.mock-checkout=true):
 * initiate returns PENDING + a real checkout URL (no synchronous capture); the hosted page's callback
 * confirms capture → HELD; a repeat callback is idempotent; a cancel callback → FAILED.
 */
@SpringBootTest(properties = "payment.gateway.mock-checkout=true")
@ActiveProfiles("dev")
class MockGatewayPaymentTest {

    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired MaintenanceCenterRepository centerRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired BookingQuoteRepository quoteRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired PaymentService service;
    @Autowired PaymentGateway gateway;
    @Autowired TransactionTemplate tx;

    private User customer;
    private MaintenanceCenter center;

    @BeforeEach
    void setUp() {
        tx.executeWithoutResult(s -> {
            String suffix = UUID.randomUUID().toString().substring(0, 8);
            customer = userRepository.save(User.builder()
                    .firstname("Mf").lastname("Cust").email("mf-cust-" + suffix + "@test.local")
                    .password(passwordEncoder.encode("Test@1234")).enabled(true).accountLocked(false)
                    .userType(UserType.CUSTOMER).build());
            center = centerRepository.save(MaintenanceCenter.builder()
                    .nameAr("مركز " + suffix).nameEn("Mf Center " + suffix)
                    .email("mf-c-" + suffix + "@test.local").phone("+96555111" + suffix.substring(0, 3))
                    .categories(new ArrayList<>()).workingDays(new ArrayList<>())
                    .specializations(new ArrayList<>()).imageUrls(new ArrayList<>())
                    .certifications(new ArrayList<>())
                    .isVerified(false).isActive(true).enabled(true).build());
        });
    }

    private Booking bookingWithApprovedQuote(String total) {
        return tx.execute(s -> {
            Booking b = bookingRepository.save(Booking.builder()
                    .bookingNumber("MF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
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
    void gatewayBean_isAsyncMock() {
        assertThat(gateway).isInstanceOf(MockMyFatoorahGateway.class);
        assertThat(gateway.capturesSynchronously()).isFalse();
    }

    @Test
    void asyncHostedCheckout_pendingThenCallbackHolds() {
        Booking b = bookingWithApprovedQuote("28.500");
        var init = service.initiate(customer, new InitiatePaymentRequestDto(
                b.getId(), PaymentMethod.KNET, false, false, null, "mf-" + b.getId()));

        // No synchronous capture — the client must open the hosted checkout.
        assertThat(init.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(init.checkoutUrl()).contains("/gateway/mock/checkout/MF-");

        String ref = paymentRepository.findById(init.paymentId()).orElseThrow().getGatewayReference();
        // The hosted page's callback (webhook equivalent) confirms capture.
        assertThat(service.confirmGatewayCallback(ref, true)).isEqualTo(PaymentStatus.HELD);
        assertThat(service.getStatus(customer, init.paymentId()).status()).isEqualTo(PaymentStatus.HELD);
        // Idempotent: a repeat callback after settlement is a no-op.
        assertThat(service.confirmGatewayCallback(ref, true)).isEqualTo(PaymentStatus.HELD);
    }

    @Test
    void cancelledCheckout_marksFailed() {
        Booking b = bookingWithApprovedQuote("10.000");
        var init = service.initiate(customer, new InitiatePaymentRequestDto(
                b.getId(), PaymentMethod.KNET, false, false, null, "mf-fail-" + b.getId()));
        String ref = paymentRepository.findById(init.paymentId()).orElseThrow().getGatewayReference();
        assertThat(service.confirmGatewayCallback(ref, false)).isEqualTo(PaymentStatus.FAILED);
        assertThat(service.getStatus(customer, init.paymentId()).status()).isEqualTo(PaymentStatus.FAILED);
    }
}
