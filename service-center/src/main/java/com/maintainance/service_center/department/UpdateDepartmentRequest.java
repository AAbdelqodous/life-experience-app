package com.maintainance.service_center.department;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDepartmentRequest {
    private String nameAr;
    private String nameEn;
    private List<Long> categoryIds;
    private Integer displayOrder;

    // Spec 022 — partial update semantics. Service layer enforces invariants.
    private Boolean isDiagnostic;

    @DecimalMin(value = "0.000", message = "Diagnostic fee must be >= 0")
    private BigDecimal diagnosticFeeAmount;
}
