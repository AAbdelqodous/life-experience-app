package com.maintainance.service_center.service;

import com.maintainance.service_center.category.ServiceCategory;
import com.maintainance.service_center.center.MaintenanceCenter;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "center_services",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"center_id", "category_id", "service_id"})
        }
)
@EntityListeners(AuditingEntityListener.class)
public class CenterService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id", nullable = false)
    private MaintenanceCenter center;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ServiceCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(precision = 10, scale = 3)
    private BigDecimal minPrice;

    @Column(precision = 10, scale = 3)
    private BigDecimal maxPrice;

    private Integer typicalDurationMinutes;

    @Column(length = 1000)
    private String descriptionAr;

    @Column(length = 1000)
    private String descriptionEn;

    @Column(nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime updatedAt;
}
