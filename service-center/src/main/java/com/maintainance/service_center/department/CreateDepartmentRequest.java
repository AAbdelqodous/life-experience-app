package com.maintainance.service_center.department;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDepartmentRequest {
    @NotBlank(message = "Arabic name is required")
    private String nameAr;

    @NotBlank(message = "English name is required")
    private String nameEn;

    private List<Long> categoryIds;

    private Integer displayOrder;

    // Spec 022 — optional flags. Service layer enforces invariants
    // (single diagnostic per center, fee only when isDiagnostic, etc.).
    private Boolean isDiagnostic;

    @DecimalMin(value = "0.000", message = "Diagnostic fee must be >= 0")
    private BigDecimal diagnosticFeeAmount;
}
