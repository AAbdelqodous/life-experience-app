package com.maintainance.service_center.quote;

import com.maintainance.service_center.booking.Booking;
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
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "booking_quote")
@EntityListeners(AuditingEntityListener.class)
public class BookingQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    private int version;

    @ElementCollection
    @CollectionTable(name = "quote_line_items", joinColumns = @JoinColumn(name = "quote_id"))
    @OrderColumn
    private List<QuoteLineItem> lineItems = new ArrayList<>();

    @Column(precision = 10, scale = 3)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    private String discountReason;

    @Column(precision = 10, scale = 3)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(precision = 10, scale = 3)
    private BigDecimal subtotal;

    @Column(precision = 10, scale = 3)
    private BigDecimal totalAmount;

    private Integer estimatedDurationMinutes;

    private String notes;

    private String notesAr;

    @Enumerated(EnumType.STRING)
    private QuoteStatus status = QuoteStatus.DRAFT;

    private LocalDateTime sentAt;

    private LocalDateTime respondedAt;

    private String responseNotes;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime updatedAt;
}
