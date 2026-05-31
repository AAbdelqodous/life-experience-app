package com.maintainance.service_center.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Dev/no-account stub for {@link PaymentGateway}. Returns a placeholder checkout URL and reports
 * synchronous capture, so the escrow flow is exercisable end-to-end without any gateway. This is the
 * default; set {@code payment.gateway.mock-checkout=true} to use the realistic, webhook-style
 * {@link MockMyFatoorahGateway} instead (or wire a real provider client in its place later).
 */
@Component
@ConditionalOnProperty(name = "payment.gateway.mock-checkout", havingValue = "false", matchIfMissing = true)
@Slf4j
public class StubPaymentGateway implements PaymentGateway {

    @Override
    public CheckoutSession createCheckout(Payment payment, BigDecimal externalAmount) {
        String reference = "STUB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String checkoutUrl = "https://gateway.stub.local/checkout/" + reference;
        log.info("[stub-gateway] checkout for payment {} amount {} → {}",
                payment.getId(), externalAmount, reference);
        return new CheckoutSession(checkoutUrl, reference);
    }

    @Override
    public boolean capturesSynchronously() {
        return true;
    }
}
