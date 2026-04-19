package com.maintainance.service_center.pricing;

import com.maintainance.service_center.booking.ServiceType;
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
@Table(name = "center_service_pricing", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"center_id", "serviceType"}))
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
    
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal minPrice;
    
    @Column(nullable = false, precision = 10, scale = 3)
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