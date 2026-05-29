package com.maintainance.service_center.quoterequest;

import com.maintainance.service_center.category.ServiceCategory;
import com.maintainance.service_center.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Spec 009 (customer) / 024 (center) — a customer quote request broadcast to matching centers.
 * Responses are sealed: the customer sees all {@link QuoteResponse}s; a center sees only its own.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "quote_request")
@EntityListeners(AuditingEntityListener.class)
public class QuoteRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ServiceCategory category;

    /** Optional specific service the customer named (nullable). */
    private Long serviceId;

    @Column(length = 2000, nullable = false)
    private String description;

    @ElementCollection
    @CollectionTable(name = "quote_request_attachments", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "url")
    private List<String> attachmentUrls = new ArrayList<>();

    private String vehicleOrApplianceNote;

    /** Preferred area governorate for matching (free text from the customer). */
    private String areaGovernorate;

    @Enumerated(EnumType.STRING)
    private FulfillmentHint fulfillmentHint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuoteRequestStatus status = QuoteRequestStatus.OPEN;

    /** Number of centers the request was broadcast to at creation. */
    private Integer reachCount = 0;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /** Set when a quote is accepted — the booking created from the winning quote. */
    private Long acceptedBookingId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime updatedAt;
}
