package com.maintainance.service_center.center;

import com.maintainance.service_center.address.AddressRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class MaintenanceCenterRequest {

    @NotBlank(message = "Arabic name is required")
    private String nameAr;

    @NotBlank(message = "English name is required")
    private String nameEn;

    private String descriptionAr;
    private String descriptionEn;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone is required")
    private String phone;

    private String alternativePhone;

    @NotNull(message = "Address is required")
    private AddressRequest address;

    private Double latitude;
    private Double longitude;

    private LocalTime openingTime;
    private LocalTime closingTime;

    private List<String> workingDays;
    private List<String> specializations;

    private String logoUrl;
    private List<String> imageUrls;
    private List<String> certifications;

    @NotNull(message = "At least one category is required")
    private List<Long> categoryIds;
}
