package com.maintainance.service_center.quote;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class QuoteLineItemRequest {

    @NotBlank(message = "Description is required")
    private String description;

    private String descriptionAr;

    @NotNull(message = "Parts cost is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Parts cost must be greater than or equal to 0")
    private BigDecimal partsCost;

    @NotNull(message = "Labor cost is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Labor cost must be greater than or equal to 0")
    private BigDecimal laborCost;

    // Spec 025 — catalogued part: when partId is set and adHoc is false, the backend snapshots
    // salePrice×quantity into partsCost (R4) and decrements stock on commit. quantity defaults to 1.
    private Long partId;

    private Integer quantity;

    private boolean adHoc;
}
