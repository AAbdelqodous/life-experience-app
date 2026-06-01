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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Spec 023 — a center's deposit policy (cut no-shows). One per center for v1. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "deposit_config")
@EntityListeners(AuditingEntityListener.class)
public class DepositConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "center_id", nullable = false, unique = true)
    private MaintenanceCenter center;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DepositMode mode = DepositMode.NONE;

    @Column(precision = 10, scale = 3)
    private BigDecimal flatAmount;

    /** 0–100, when mode == PERCENT. */
    private Integer percent;

    /** Null = center-wide default; otherwise the service it applies to. */
    private Long appliesToServiceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CancellationPolicy cancellationPolicy = CancellationPolicy.RETAIN;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime updatedAt;
}
