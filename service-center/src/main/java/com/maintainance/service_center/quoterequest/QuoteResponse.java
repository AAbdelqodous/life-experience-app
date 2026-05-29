package com.maintainance.service_center.quoterequest;

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

/**
 * Spec 024 — one center's (sealed) quote on a {@link QuoteRequest}. At most one per (request, center).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "quote_response",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_quote_response_request_center",
                columnNames = {"request_id", "center_id"}
        )
)
@EntityListeners(AuditingEntityListener.class)
public class QuoteResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private QuoteRequest request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "center_id", nullable = false)
    private MaintenanceCenter center;

    @Column(precision = 10, scale = 3, nullable = false)
    private BigDecimal priceMin;

    /** Equals priceMin for a fixed price. */
    @Column(precision = 10, scale = 3, nullable = false)
    private BigDecimal priceMax;

    private Integer estimatedDurationMinutes;

    @Column(length = 1000)
    private String inclusions;

    @Column(length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuoteResponseStatus status = QuoteResponseStatus.SUBMITTED;

    private LocalDateTime submittedAt;

    private LocalDateTime updatedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
