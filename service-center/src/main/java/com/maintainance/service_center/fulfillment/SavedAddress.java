package com.maintainance.service_center.fulfillment;

import com.maintainance.service_center.booking.ServiceAddress;
import com.maintainance.service_center.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/** Spec 008 — a customer's saved service address (Home/Work/Other), reused across bookings (R4). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "saved_address", indexes = @Index(name = "idx_saved_address_customer", columnList = "customer_id"))
@EntityListeners(AuditingEntityListener.class)
public class SavedAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Embedded
    private ServiceAddress address;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
