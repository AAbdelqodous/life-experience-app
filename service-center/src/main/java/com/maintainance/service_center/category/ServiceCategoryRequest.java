package com.maintainance.service_center.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceCategoryRequest {

    @NotBlank(message = "Category code is required")
    private String code;

    @NotBlank(message = "Arabic name is required")
    private String nameAr;

    @NotBlank(message = "English name is required")
    private String nameEn;

    private String descriptionAr;

    private String descriptionEn;

    private String iconUrl;

    private Integer displayOrder;

    private Boolean isActive;

    private Long parentId;
}
