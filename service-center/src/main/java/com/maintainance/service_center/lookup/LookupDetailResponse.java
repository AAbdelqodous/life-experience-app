package com.maintainance.service_center.lookup;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LookupDetailResponse {
    private Long id;
    private String code;
    private String nameEn;
    private String nameAr;
    private String shortName;
    private Integer sortOrder;
    private Boolean isActive;
    private Boolean isSystem;
    private Long parentId;
    private String parentCode;
    private String extraData;
    private LocalDateTime createdDate;
    private String createdBy;
    private LocalDateTime updatedDate;
    private String updatedBy;
}
