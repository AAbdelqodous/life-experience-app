package com.maintainance.service_center.booking;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
    name = "booking_claim_audit",
    indexes = {
        @Index(name = "idx_claim_audit_booking", columnList = "booking_id"),
        @Index(name = "idx_claim_audit_membership", columnList = "membership_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
public class BookingClaimAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "membership_id", nullable = false)
    private Long membershipId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime claimedAt;
}
