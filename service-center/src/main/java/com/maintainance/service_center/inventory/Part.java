package com.maintainance.service_center.inventory;

import com.maintainance.service_center.center.MaintenanceCenter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Spec 025 — a catalog part stocked by a center. {@code onHand} is a cached running total kept in
 * step with the {@link StockMovement} ledger (R2); consumption decrements it atomically (R3).
 * Unique {@code (center_id, sku)}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "part", uniqueConstraints = @UniqueConstraint(name = "uk_part_center_sku",
        columnNames = {"center_id", "sku"}))
@EntityListeners(AuditingEntityListener.class)
public class Part {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "center_id", nullable = false)
    private MaintenanceCenter center;

    @Column(nullable = false)
    private String nameAr;

    @Column(nullable = false)
    private String nameEn;

    @Column(nullable = false)
    private String sku;

    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Unit unit = Unit.PIECE;

    @Column(precision = 10, scale = 3, nullable = false)
    private BigDecimal costPrice;

    @Column(precision = 10, scale = 3, nullable = false)
    private BigDecimal salePrice;

    private String supplier;

    @Column(nullable = false)
    private int reorderThreshold;

    /** Cached on-hand quantity; equals Σ(movement.quantity). Decremented atomically on consume. */
    @Column(name = "on_hand", nullable = false)
    private int onHand;

    @Column(nullable = false)
    private boolean isActive = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime updatedAt;
}
