package com.maintainance.service_center.lookup;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LookupResponse {
    private Long id;
    private String code;
    private String nameEn;
    private String nameAr;
    private String description;
    private Boolean isActive;
    private Boolean isSystem;
    private Long version;
    private List<LookupDetailResponse> details;
    private LocalDateTime createdDate;
    private String createdBy;
    private LocalDateTime updatedDate;
    private String updatedBy;
}
