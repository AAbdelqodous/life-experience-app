package com.maintainance.service_center.department;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDepartmentRequest {
    private String nameAr;
    private String nameEn;
    private List<Long> categoryIds;
    private Integer displayOrder;
}
