package com.maintainance.service_center.center;

import com.maintainance.service_center.address.Address;
import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.category.ServiceCategory;
import com.maintainance.service_center.review.Review;
import com.maintainance.service_center.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
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
@Table(name = "maintenance_centers")
@EntityListeners(AuditingEntityListener.class)
public class MaintenanceCenter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nameAr;

    @Column(nullable = false)
    private String nameEn;

    @Column(length = 1000)
    private String descriptionAr;

    @Column(length = 1000)
    private String descriptionEn;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phone;

    private String alternativePhone;

    @Embedded
    private Address address;

    private Double latitude;
    private Double longitude;

    private LocalTime openingTime;
    private LocalTime closingTime;

    @ElementCollection
    @CollectionTable(name = "center_working_days")
    private List<String> workingDays = new ArrayList<>();

    @Column(precision = 3, scale = 2)
    private BigDecimal averageRating = BigDecimal.ZERO;

    private Integer totalReviews = 0;

    private Boolean isVerified = false;
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToMany
    @JoinTable(
            name = "service_category",
            joinColumns = @JoinColumn(name = "center_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private List<ServiceCategory> categories = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "center_specializations")
    private List<String> specializations = new ArrayList<>();

    private String logoUrl;

    @ElementCollection
    @CollectionTable(name = "center_images")
    private List<String> imageUrls = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "center_certifications")
    private List<String> certifications = new ArrayList<>();

    @OneToMany(mappedBy = "center", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "center", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Booking> bookings = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime updatedAt;
}
