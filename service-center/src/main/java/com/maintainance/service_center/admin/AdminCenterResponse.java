package com.maintainance.service_center.admin;

import com.maintainance.service_center.address.Address;
import com.maintainance.service_center.center.MaintenanceCenterResponse;
import com.maintainance.service_center.user.ApprovalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCenterResponse {
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
    private Boolean enabled;
    private Integer ownerId;
    private String ownerName;
    private String ownerEmail;
    private ApprovalStatus approvalStatus;
    private List<MaintenanceCenterResponse.CategorySummary> categories;
    private List<String> specializations;
    private String logoUrl;
    private List<String> imageUrls;
    private List<String> certifications;
    private LocalDateTime createdAt;
}
