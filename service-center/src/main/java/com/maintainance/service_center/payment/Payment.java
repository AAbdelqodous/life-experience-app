package com.maintainance.service_center.payment;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Spec 007 (customer) / 023 (center) — a money-movement against a booking, held in escrow until
 * the customer releases (or auto-release fires). Commission is snapshotted at capture (FR-010).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payment")
@EntityListeners(AuditingEntityListener.class)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "center_id", nullable = false)
    private MaintenanceCenter center;

    @Column(precision = 10, scale = 3, nullable = false)
    private BigDecimal grossAmount;

    /** Wallet portion applied (KD); remainder is charged externally. */
    @Column(precision = 10, scale = 3)
    private BigDecimal walletAmount = BigDecimal.ZERO;

    /** Platform commission rate snapshotted at capture (e.g. 0.0500). */
    @Column(precision = 5, scale = 4)
    private BigDecimal commissionRate;

    @Column(precision = 10, scale = 3)
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    @Column(precision = 10, scale = 3)
    private BigDecimal netAmount = BigDecimal.ZERO;

    @Column(precision = 10, scale = 3)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    /** Client-supplied, unique per attempt — guarantees idempotent initiation (FR-011). */
    @Column(unique = true)
    private String idempotencyKey;

    private String gatewayReference;

    private boolean disputed = false;

    private String disputeReason;

    /** Set by the center's "mark work complete" action (spec 023); gates customer release. */
    private boolean releaseEligible = false;

    /** When held funds auto-release if the customer takes no action. */
    private LocalDateTime autoReleaseAt;

    private LocalDateTime capturedAt;

    private LocalDateTime releasedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime updatedAt;
}
