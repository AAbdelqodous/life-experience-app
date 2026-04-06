package com.maintainance.service_center.review;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "review",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "center_id"}))
@EntityListeners(AuditingEntityListener.class)
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id", nullable = false)
    private MaintenanceCenter center;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = true)
    private Booking booking; // Optional: Link to the service booking

    @Column(nullable = false)
    private Integer rating; // 1-5 stars

    @Column(length = 2000)
    private String comment;

    // Specific ratings for different aspects
    private Integer serviceQuality; // 1-5
    private Integer priceValue; // 1-5
    private Integer timeEfficiency; // 1-5
    private Integer customerService; // 1-5
    private Integer cleanliness; // 1-5

    private String serviceType;
    private Double serviceCost;

    // Review media
    @ElementCollection
    @CollectionTable(name = "review_images")
    private List<String> imageUrls = new ArrayList<>();

    // Review interaction
    private Integer helpfulCount = 0; // How many users found this review helpful
    private Boolean isVerified = false; // Verified purchase/service
    private Boolean isRecommended = true; // Would recommend to others

    private String centerResponse;
    private LocalDateTime centerResponseDate;

    // Moderation
    private Boolean isApproved = true;
    private Boolean isFlagged = false;
    private String flagReason;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime updatedAt;
}
