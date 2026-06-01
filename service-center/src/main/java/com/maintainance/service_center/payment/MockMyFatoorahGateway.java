package com.maintainance.service_center.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Spec 007 — a realistic, MyFatoorah-shaped mock of the hosted-checkout gateway. Unlike the
 * {@link StubPaymentGateway} (synchronous, placeholder URL), this returns {@code capturesSynchronously
 * = false} and a real, openable checkout URL — a backend-served hosted page (see
 * {@code GatewayMockController}) the client opens in a WebView. Capture is confirmed asynchronously via
 * that page's callback, exactly like the real provider's webhook/redirect. No external account needed.
 *
 * <p>Enabled with {@code payment.gateway.mock-checkout=true}; off by default so the stub (and the test
 * suite's synchronous expectations) remain the default. Swapping in the real MyFatoorah client later is
 * a like-for-like replacement of this one bean — the {@link PaymentService} flow is unchanged.
 */
@Component("realPaymentGateway")
@ConditionalOnProperty(name = "payment.gateway.mock-checkout", havingValue = "true")
@Slf4j
public class MockMyFatoorahGateway implements PaymentGateway {

    /** External base URL the WebView can reach (Android emulator default). Includes the context path. */
    @Value("${payment.gateway.public-base-url:http://10.0.2.2:8080/api/v1}")
    private String publicBaseUrl;

    @Override
    public CheckoutSession createCheckout(Payment payment, BigDecimal externalAmount) {
        // MyFatoorah-shaped: an InvoiceId-like reference + a hosted PaymentURL the client opens.
        String reference = "MF-" + payment.getId() + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String checkoutUrl = publicBaseUrl.replaceAll("/+$", "") + "/gateway/mock/checkout/" + reference;
        log.info("[mock-myfatoorah] hosted checkout for payment {} amount {} KD → {}",
                payment.getId(), externalAmount, checkoutUrl);
        return new CheckoutSession(checkoutUrl, reference);
    }

    @Override
    public boolean capturesSynchronously() {
        return false; // capture is confirmed by the hosted page's callback (mirrors the real webhook)
    }
}
