package com.maintainance.service_center.lookup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LookupDetailRequest {

    @NotBlank(message = "Code is required")
    @Size(max = 100, message = "Code must not exceed 100 characters")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "Code must be UPPER_SNAKE_CASE (e.g. CUSTOMER)")
    private String code;

    @NotBlank(message = "English name is required")
    @Size(max = 500, message = "English name must not exceed 500 characters")
    private String nameEn;

    @NotBlank(message = "Arabic name is required")
    @Size(max = 500, message = "Arabic name must not exceed 500 characters")
    private String nameAr;

    @Size(max = 200, message = "Short name must not exceed 200 characters")
    private String shortName;

    private Integer sortOrder = 0;
    private Boolean isActive = true;
    private Boolean isSystem = false;
    private Long parentId;
    private String extraData;
}
