package com.maintainance.service_center.center;

import com.maintainance.service_center.address.Address;
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
    private String descriptionAr;
    private String descriptionEn;
    private String cityAr;
    private String cityEn;
    private String phone;
    private Address address;
    private Double latitude;
    private Double longitude;
    private BigDecimal averageRating;
    private Integer totalReviews;
    private Boolean isVerified;
    private Boolean isActive;
    private String logoUrl;
    private List<String> workingDays;
    private Long categoryId;
}
