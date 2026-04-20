package com.maintainance.service_center.booking;

import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.progress.WorkStage;
import com.maintainance.service_center.review.Review;
import com.maintainance.service_center.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "booking")
@EntityListeners(AuditingEntityListener.class)
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String bookingNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id", nullable = false)
    private MaintenanceCenter center;

    @Column(nullable = false)
    private LocalDate bookingDate;

    @Column(nullable = false)
    private LocalTime bookingTime;

    private LocalTime estimatedEndTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus bookingStatus = BookingStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private ServiceType serviceType;

    @Column(length = 500)
    private String serviceDescription;

    @Column(length = 1000)
    private String problemDescription;

    @ElementCollection
    @CollectionTable(name = "booking_services")
    private List<String> requestedServices = new ArrayList<>();

    // Device/Vehicle information
    private String deviceType; // Car model, device brand, etc.
    private String deviceModel;
    private String deviceYear;
    private String deviceSerial;

    @ElementCollection
    @CollectionTable(name = "booking_problem_images")
    private List<String> problemImageUrls = new ArrayList<>();

    private BigDecimal estimatedCost;
    private BigDecimal finalCost;
    private String costNotes;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    private LocalDateTime paidAt;

    private LocalDateTime completedAt;
    private String completionNotes;

    @ElementCollection
    @CollectionTable(name = "booking_completion_images")
    private List<String> completionImageUrls = new ArrayList<>();

    private LocalDateTime cancelledAt;
    private String cancelledReason;

    @Enumerated(EnumType.STRING)
    private CancelledBy cancelledBy;

    private String customerPhone;
    private String customerAlternativePhone;
    private String customerAddress;
    private String specialInstructions;

    private Boolean isUrgent;
    private Boolean pickupRequired;
    private String pickupAddress;

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL)
    private Review review;

    private Boolean reminderSent;
    private LocalDateTime reminderSentAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private WorkStage currentWorkStage;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime updatedAt;
}
