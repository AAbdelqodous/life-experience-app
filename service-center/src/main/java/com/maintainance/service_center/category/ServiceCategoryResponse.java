package com.maintainance.service_center.category;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ServiceCategoryResponse {
    private Long id;
    private String code;
    private String nameAr;
    private String nameEn;
    private String descriptionAr;
    private String descriptionEn;
    private String iconUrl;
    private Integer displayOrder;
    private Boolean isActive;
    private Long parentId;
    private String parentNameAr;
    private String parentNameEn;
    private Integer subcategoryCount;
    private Integer centerCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
