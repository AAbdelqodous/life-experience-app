package com.maintainance.service_center.service;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateCenterServiceRequest {

    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer typicalDurationMinutes;
    private String descriptionAr;
    private String descriptionEn;
}
