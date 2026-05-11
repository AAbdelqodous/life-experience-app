package com.maintainance.service_center.service;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateCenterServiceRequest {

    @NotNull(message = "categoryId is required")
    private Long categoryId;

    @NotNull(message = "serviceId is required")
    private Long serviceId;

    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer typicalDurationMinutes;
    private String descriptionAr;
    private String descriptionEn;
}
