package com.maintainance.service_center.center;

import com.maintainance.service_center.address.Address;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@Builder
public class MaintenanceCenterResponse {
    private Long id;
    private String nameAr;
    private String nameEn;
    private String descriptionAr;
    private String descriptionEn;
    private String email;
    private String phone;
    private String alternativePhone;
    private Address address;
    private Double latitude;
    private Double longitude;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private List<String> workingDays;
    private BigDecimal averageRating;
    private Integer totalReviews;
    private Boolean isVerified;
    private Boolean isActive;
    private Integer ownerId;
    private String ownerName;
    private List<CategorySummary> categories;
    private List<String> specializations;
    private String logoUrl;
    private List<String> imageUrls;
    private List<String> certifications;
    private LocalDateTime createdAt;

    @Getter
    @Builder
    public static class CategorySummary {
        private Long id;
        private String nameAr;
        private String nameEn;
        private String code;
    }
}
