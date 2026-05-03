package com.maintainance.service_center.offer;

import com.maintainance.service_center.booking.ServiceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class OfferRequest {

    @NotBlank(message = "Arabic title is mandatory")
    private String titleAr;

    @NotBlank(message = "English title is mandatory")
    private String titleEn;

    private String descriptionAr;
    private String descriptionEn;

    @NotNull(message = "Discount type is mandatory")
    private DiscountType discountType;

    @NotNull(message = "Discount value is mandatory")
    @DecimalMin(value = "0.001", message = "Discount value must be greater than 0")
    private BigDecimal discountValue;

    private List<ServiceType> applicableServiceTypes = new ArrayList<>();

    @NotNull(message = "Start date is mandatory")
    private LocalDate startDate;

    @NotNull(message = "End date is mandatory")
    private LocalDate endDate;

    @Min(value = 1, message = "Max redemptions must be at least 1")
    private Integer maxRedemptions;
}
