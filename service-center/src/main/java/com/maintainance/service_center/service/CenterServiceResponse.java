package com.maintainance.service_center.service;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class CenterServiceResponse {
    private Long id;
    private CategoryInfo category;
    private ServiceInfo service;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer typicalDurationMinutes;
    private String descriptionAr;
    private String descriptionEn;
    private Boolean isActive;
    private LocalDateTime createdAt;

    @Getter
    @Builder
    public static class CategoryInfo {
        private Long id;
        private String code;
        private String nameAr;
        private String nameEn;
    }

    @Getter
    @Builder
    public static class ServiceInfo {
        private Long id;
        private String code;
        private String nameAr;
        private String nameEn;
    }
}
