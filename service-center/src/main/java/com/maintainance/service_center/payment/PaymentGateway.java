package com.maintainance.service_center.payment;

import java.math.BigDecimal;

/**
 * Spec 007 — abstraction over the hosted-checkout gateway (MyFatoorah / Tap). Kept behind an
 * interface so the escrow/payment flow is independent of the provider; a real implementation
 * confirms capture via a webhook. See {@link StubPaymentGateway} for the dev/no-account stub.
 */
public interface PaymentGateway {

    /** A hosted-checkout session the client opens in a WebView. */
    record CheckoutSession(String checkoutUrl, String reference) {}

    /** Create a hosted-checkout session for the external (non-wallet) amount. */
    CheckoutSession createCheckout(Payment payment, BigDecimal externalAmount);

    /**
     * Whether this gateway captures synchronously at initiation (stub/dev) rather than via an
     * asynchronous webhook. Real gateways return false and confirm capture through the webhook.
     */
    boolean capturesSynchronously();
}
