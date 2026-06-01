package com.maintainance.service_center.payment;

import com.maintainance.service_center.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Spec 007 — a tokenized saved card. Only the gateway token + a masked label are stored;
 * the raw PAN never touches the platform (FR-010).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "saved_payment_method")
@EntityListeners(AuditingEntityListener.class)
public class SavedMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    private String brand;

    /** e.g. "•••• 4242" — never the full number. */
    private String maskedLabel;

    /** "MM/YY". */
    private String expiry;

    /** Opaque gateway token (never a PAN). */
    @Column(nullable = false)
    private String gatewayToken;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
