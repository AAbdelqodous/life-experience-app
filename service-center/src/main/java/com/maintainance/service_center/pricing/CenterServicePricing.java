package com.maintainance.service_center.pricing;

import com.maintainance.service_center.booking.ServiceType;
import com.maintainance.service_center.center.MaintenanceCenter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "center_service_pricing",
       uniqueConstraints = @UniqueConstraint(columnNames = {"center_id", "service_type", "service_name_en"}))
@EntityListeners(AuditingEntityListener.class)
public class CenterServicePricing {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id", nullable = false)
    private MaintenanceCenter center;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceType serviceType;
    
    @Column(nullable = false)
    private String serviceNameAr;
    
    @Column(nullable = false)
    private String serviceNameEn;
    
    @Column(precision = 10, scale = 3, nullable = false)
    private BigDecimal minPrice;
    
    @Column(precision = 10, scale = 3, nullable = false)
    private BigDecimal maxPrice;
    
    private Integer typicalDurationMinutes;
    
    private String descriptionAr;
    
    private String descriptionEn;
    
    @Column(nullable = false)
    private boolean isActive = true;
    
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}