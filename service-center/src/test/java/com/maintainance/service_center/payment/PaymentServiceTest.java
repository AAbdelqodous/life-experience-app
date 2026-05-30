package com.maintainance.service_center.payment;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.booking.BookingStatus;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.quote.BookingQuote;
import com.maintainance.service_center.quote.BookingQuoteRepository;
import com.maintainance.service_center.quote.QuoteLineItem;
import com.maintainance.service_center.quote.QuoteStatus;
import com.maintainance.service_center.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Spec 007 — locks the payment/escrow logic without a DB (Mockito). */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock WalletRepository walletRepository;
    @Mock WalletTransactionRepository walletTxRepository;
    @Mock SavedMethodRepository savedMethodRepository;
    @Mock BookingRepository bookingRepository;
    @Mock BookingQuoteRepository quoteRepository;
    @Mock PaymentGateway gateway;

    @InjectMocks PaymentService service;

    private User customer;
    private Booking booking;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setId(1);
        MaintenanceCenter center = new MaintenanceCenter();
        center.setId(10L);
        booking = new Booking();
        booking.setId(500L);
        booking.setCustomer(customer);
        booking.setCenter(center);
        booking.setBookingStatus(BookingStatus.PENDING);
    }

    private BookingQuote approvedQuote(String total) {
        QuoteLineItem li = new QuoteLineItem();
        li.setDescription("AC repair");
        li.setDescriptionAr("إصلاح مكيف");
        li.setPartsCost(new BigDecimal("18.000"));
        li.setLaborCost(new BigDecimal("10.500"));
        BookingQuote q = new BookingQuote();
        q.setStatus(QuoteStatus.APPROVED);
        q.setTotalAmount(new BigDecimal(total));
        q.setLineItems(List.of(li));
        return q;
    }

    @Test
    void getInvoice_derivesLinesFromApprovedQuote() {
        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));
        when(quoteRepository.findByBookingIdOrderByVersionDesc(500L)).thenReturn(List.of(approvedQuote("28.500")));
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(500L)).thenReturn(List.of());

        BookingInvoiceResponse inv = service.getInvoice(customer, 500L);

        assertThat(inv.lines()).hasSize(1);
        assertThat(inv.total()).isEqualByComparingTo("28.500");
        assertThat(inv.paymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(inv.releaseEligible()).isFalse();
        assertThat(inv.availableMethods()).contains(PaymentMethod.KNET);
    }

    @Test
    void initiate_external_stubCaptures_HELD_withCommissionSnapshot() {
        when(paymentRepository.findByIdempotencyKey("k1")).thenReturn(Optional.empty());
        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));
        when(quoteRepository.findByBookingIdOrderByVersionDesc(500L)).thenReturn(List.of(approvedQuote("28.500")));
        when(gateway.createCheckout(any(), any()))
                .thenReturn(new PaymentGateway.CheckoutSession("https://gw/checkout/REF", "REF"));
        when(gateway.capturesSynchronously()).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            if (p.getId() == null) p.setId(9001L);
            return p;
        });

        var res = service.initiate(customer, new InitiatePaymentRequestDto(
                500L, PaymentMethod.KNET, false, false, null, "k1"));

        assertThat(res.status()).isEqualTo(PaymentStatus.HELD);
        assertThat(res.checkoutUrl()).contains("/checkout/");
        // commission = 28.500 * 0.050 = 1.425
    }

    @Test
    void initiate_isIdempotent() {
        Payment existing = new Payment();
        existing.setId(9001L);
        existing.setStatus(PaymentStatus.HELD);
        existing.setGatewayReference("REF");
        when(paymentRepository.findByIdempotencyKey("k1")).thenReturn(Optional.of(existing));

        var res = service.initiate(customer, new InitiatePaymentRequestDto(
                500L, PaymentMethod.KNET, false, false, null, "k1"));

        assertThat(res.paymentId()).isEqualTo(9001L);
        assertThat(res.status()).isEqualTo(PaymentStatus.HELD);
    }

    @Test
    void release_blockedUntilMarkedComplete() {
        Payment held = new Payment();
        held.setId(9001L);
        held.setStatus(PaymentStatus.HELD);
        held.setBooking(booking);
        held.setReleaseEligible(false);
        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(500L)).thenReturn(List.of(held));

        // not marked complete → conflict
        assertThatThrownBy(() -> service.release(customer, 500L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not marked complete");

        // center marked complete → release-eligible → released
        held.setReleaseEligible(true);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        var res = service.release(customer, 500L);
        assertThat(res.paymentStatus()).isEqualTo(PaymentStatus.RELEASED);
        assertThat(held.getReleasedAt()).isNotNull();
    }

    @Test
    void topUp_creditsWalletSynchronously() {
        Wallet wallet = new Wallet();
        wallet.setId(1L);
        wallet.setUser(customer);
        wallet.setBalance(new BigDecimal("5.000"));
        when(walletRepository.findByUserId(1)).thenReturn(Optional.of(wallet));
        when(gateway.capturesSynchronously()).thenReturn(true);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));

        var res = service.topUp(customer, new TopUpRequestDto(new BigDecimal("10.000"), PaymentMethod.KNET, "t1"));

        assertThat(res.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(res.checkoutUrl()).isNull();
        assertThat(wallet.getBalance()).isEqualByComparingTo("15.000");
    }
}
