package com.maintainance.service_center.center;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class MaintenanceCenterSummaryResponse {
    private Long id;
    private String nameAr;
    private String nameEn;
    private String cityAr;
    private String cityEn;
    private BigDecimal averageRating;
    private Integer totalReviews;
    private Boolean isVerified;
    private String logoUrl;
    private List<String> workingDays;
}
