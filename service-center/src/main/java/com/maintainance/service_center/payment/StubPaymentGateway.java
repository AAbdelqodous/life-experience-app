package com.maintainance.service_center.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Dev/no-account stub for {@link PaymentGateway}. Returns a placeholder checkout URL and reports
 * synchronous capture, so the escrow flow is exercisable end-to-end without a live MyFatoorah/Tap
 * merchant account. Replace with a real, webhook-driven implementation (capturesSynchronously=false)
 * once a gateway account + HTTPS are provisioned. Backed off automatically if a real bean is present.
 */
@Component
@ConditionalOnMissingBean(name = "realPaymentGateway")
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
