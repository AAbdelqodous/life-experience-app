package com.maintainance.service_center.lookup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LookupRequest {

    @NotBlank(message = "Code is required")
    @Size(max = 100, message = "Code must not exceed 100 characters")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "Code must be UPPER_SNAKE_CASE (e.g. USER_TYPE)")
    private String code;

    @NotBlank(message = "English name is required")
    @Size(max = 200, message = "English name must not exceed 200 characters")
    private String nameEn;

    @NotBlank(message = "Arabic name is required")
    @Size(max = 200, message = "Arabic name must not exceed 200 characters")
    private String nameAr;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private Boolean isActive = true;
    private Boolean isSystem = false;
}
