package com.maintainance.service_center.payment;

import com.maintainance.service_center.center.MaintenanceCenter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/** Spec 023 — a center's bank account for payouts (Kuwaiti IBAN). One per center for v1. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payout_account")
@EntityListeners(AuditingEntityListener.class)
public class PayoutAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "center_id", nullable = false, unique = true)
    private MaintenanceCenter center;

    @Column(nullable = false)
    private String iban;

    @Column(nullable = false)
    private String holderName;

    private String bankName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayoutAccountStatus status = PayoutAccountStatus.UNVERIFIED;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime updatedAt;
}
