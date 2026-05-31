package com.maintainance.service_center.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** Idempotent initiation: reuse an existing attempt for the same key. */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    /** Resolve a payment from the gateway's reference — used by the hosted-checkout callback/webhook. */
    Optional<Payment> findByGatewayReference(String gatewayReference);

    /** Active payment(s) for a booking, newest first. */
    List<Payment> findByBookingIdOrderByCreatedAtDesc(Long bookingId);

    /** All payments for a center — aggregated into earnings balances (spec 023). */
    List<Payment> findByCenterId(Long centerId);

    /** Auto-release sweep: held payments whose window has elapsed and not disputed. */
    List<Payment> findByStatusAndDisputedFalseAndAutoReleaseAtBefore(
            PaymentStatus status, java.time.LocalDateTime cutoff);
}
