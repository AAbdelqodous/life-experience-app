package com.maintainance.service_center.quoterequest;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTOs for the quote-request module (spec 009 customer + 024 center).
 * Package-private records — used by the controllers/service in this package.
 */
final class QuoteRequestDtos {
    private QuoteRequestDtos() {}
}

/** Customer creates + broadcasts a request. */
record CreateQuoteRequestRequest(
        @NotNull Long categoryId,
        Long serviceId,
        @NotBlank String description,
        List<String> attachmentUrls,
        String vehicleOrApplianceNote,
        String areaGovernorate,
        FulfillmentHint fulfillmentHint
) {}

/** Center submits or edits its quote. */
record SubmitQuoteRequestDto(
        @NotNull @DecimalMin(value = "0.001") BigDecimal priceMin,
        @NotNull @DecimalMin(value = "0.001") BigDecimal priceMax,
        Integer estimatedDurationMinutes,
        String inclusions,
        String message
) {}

/** Customer accepts one quote. */
record AcceptQuoteRequestDto(@NotNull Long quoteId) {}

/** Start/open a request-scoped chat. Customer supplies the centerId; center callers omit it. */
record StartChatRequest(Long centerId) {}
