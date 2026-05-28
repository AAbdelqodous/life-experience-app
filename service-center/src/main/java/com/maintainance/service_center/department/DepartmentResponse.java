package com.maintainance.service_center.department;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentResponse {
    private Long id;
    private Long centerId;
    private String nameAr;
    private String nameEn;
    private Integer displayOrder;
    private Boolean isActive;
    private List<Long> categoryIds;
    private Integer memberCount;
    // Spec 022 — null/false on existing departments before this migration.
    private Boolean isDiagnostic;
    private BigDecimal diagnosticFeeAmount;
}
