package com.maintainance.service_center.quote;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class CreateQuoteRequest {

    @NotEmpty(message = "At least one line item is required")
    @Valid
    private List<QuoteLineItemRequest> lineItems;

    private BigDecimal discountAmount;

    private String discountReason;

    private Integer estimatedDurationMinutes;

    private String notes;

    private String notesAr;
}
