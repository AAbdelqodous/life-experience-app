package com.maintainance.service_center.pricing;

import com.maintainance.service_center.booking.ServiceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CenterServicePricingRequest {
    
    @NotNull(message = "Service type is required")
    private ServiceType serviceType;
    
    @NotBlank(message = "Service name (Arabic) is required")
    private String serviceNameAr;
    
    @NotBlank(message = "Service name (English) is required")
    private String serviceNameEn;
    
    @NotNull(message = "Minimum price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Minimum price must be greater than or equal to 0")
    private BigDecimal minPrice;
    
    @NotNull(message = "Maximum price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Maximum price must be greater than or equal to 0")
    private BigDecimal maxPrice;
    
    private Integer typicalDurationMinutes;
    
    private String descriptionAr;
    
    private String descriptionEn;
    
    private Boolean isActive;
}
