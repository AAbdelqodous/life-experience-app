package com.maintainance.service_center.service;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ServiceResponse {
    private Long id;
    private String code;
    private String nameAr;
    private String nameEn;
    private String descriptionAr;
    private String descriptionEn;
    private String iconUrl;
    private Boolean isActive;
}
