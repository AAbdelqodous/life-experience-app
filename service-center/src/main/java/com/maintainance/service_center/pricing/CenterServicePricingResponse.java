package com.maintainance.service_center.pricing;

import com.maintainance.service_center.booking.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CenterServicePricingResponse {
    
    private Long id;
    private ServiceType serviceType;
    private String serviceNameAr;
    private String serviceNameEn;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer typicalDurationMinutes;
    private String descriptionAr;
    private String descriptionEn;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}