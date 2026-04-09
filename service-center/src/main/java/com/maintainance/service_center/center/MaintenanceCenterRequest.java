package com.maintainance.service_center.center;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.maintainance.service_center.address.AddressRequest;
import com.maintainance.service_center.config.LocalTimeDeserializer;
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

    @JsonFormat(pattern = "HH:mm:ss")
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    private LocalTime openingTime;

    @JsonFormat(pattern = "HH:mm:ss")
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    private LocalTime closingTime;
    private Boolean isActive;

    private List<String> workingDays;
    private List<String> specializations;

    private String logoUrl;
    private List<String> imageUrls;
    private List<String> certifications;

    @NotNull(message = "At least one category is required")
    private List<Long> categoryIds;
}
