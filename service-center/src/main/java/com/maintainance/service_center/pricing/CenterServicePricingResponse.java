package com.maintainance.service_center.pricing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.maintainance.service_center.booking.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
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
    @JsonProperty("isActive")
    private boolean active;
    private String createdAt;
    private String updatedAt;
    
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    public static CenterServicePricingResponse from(CenterServicePricing entity) {
        CenterServicePricingResponse response = new CenterServicePricingResponse();
        response.setId(entity.getId());
        response.setServiceType(entity.getServiceType());
        response.setServiceNameAr(entity.getServiceNameAr());
        response.setServiceNameEn(entity.getServiceNameEn());
        response.setMinPrice(entity.getMinPrice());
        response.setMaxPrice(entity.getMaxPrice());
        response.setTypicalDurationMinutes(entity.getTypicalDurationMinutes());
        response.setDescriptionAr(entity.getDescriptionAr());
        response.setDescriptionEn(entity.getDescriptionEn());
        response.setActive(entity.isActive());
        response.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().format(ISO_FORMATTER) : null);
        response.setUpdatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().format(ISO_FORMATTER) : null);
        return response;
    }
}