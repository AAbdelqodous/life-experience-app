package com.maintainance.service_center.offer;

import com.maintainance.service_center.booking.ServiceType;
import com.maintainance.service_center.center.MaintenanceCenter;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "center_offers")
@EntityListeners(AuditingEntityListener.class)
public class CenterOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id", nullable = false)
    private MaintenanceCenter center;

    @Column(nullable = false)
    private String titleAr;

    @Column(nullable = false)
    private String titleEn;

    @Column(length = 1000)
    private String descriptionAr;

    @Column(length = 1000)
    private String descriptionEn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal discountValue;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "center_offer_service_types",
            joinColumns = @JoinColumn(name = "center_offer_id")
    )
    @Column(name = "service_type")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private List<ServiceType> applicableServiceTypes = new ArrayList<>();

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private Integer maxRedemptions;

    @Column(nullable = false)
    @Builder.Default
    private int currentRedemptions = 0;

    private LocalDateTime cancelledAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime updatedAt;
}
