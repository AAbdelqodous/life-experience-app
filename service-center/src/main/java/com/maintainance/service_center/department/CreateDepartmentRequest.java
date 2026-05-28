package com.maintainance.service_center.department;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
