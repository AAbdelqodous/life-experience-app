package com.maintainance.service_center.quoterequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTOs (views) for the quote-request module. Package-private records.
 * Customer views expose all responses; center views are sealed to the center's own quote.
 */
final class QuoteRequestViews {
    private QuoteRequestViews() {}
}

/** Customer "my requests" list row. */
record QuoteRequestSummaryResponse(
        Long id,
        String categoryNameAr,
        String categoryNameEn,
        QuoteRequestStatus status,
        Integer reachCount,
        int responseCount,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {}

/** One center's quote, customer view (with center identity). */
record CustomerQuoteResponseDto(
        Long id,
        Long centerId,
        String centerNameAr,
        String centerNameEn,
        String centerLogoUrl,
        BigDecimal rating,
        Integer trustScore,
        Double distance,
        BigDecimal priceMin,
        BigDecimal priceMax,
        Integer estimatedDurationMinutes,
        String inclusions,
        String message,
        QuoteResponseStatus state,
        LocalDateTime respondedAt
) {}

/** Full request + all responses (customer view). */
record QuoteRequestResponse(
        Long id,
        Long categoryId,
        String categoryNameAr,
        String categoryNameEn,
        Long serviceId,
        String description,
        List<String> attachmentUrls,
        String areaGovernorate,
        FulfillmentHint fulfillmentHint,
        QuoteRequestStatus status,
        Integer reachCount,
        LocalDateTime expiresAt,
        Long acceptedBookingId,
        LocalDateTime createdAt,
        List<CustomerQuoteResponseDto> responses
) {}

/** Result of accepting a quote. */
record AcceptResultResponse(Long requestId, QuoteRequestStatus state, Long acceptedBookingId) {}

/** Result of cancelling a request. */
record CancelResultResponse(Long requestId, QuoteRequestStatus state) {}

/** Center inbox list row. `myResponseStatus` is "NONE" when this center hasn't quoted. */
record InboxItemResponse(
        Long requestId,
        String categoryNameAr,
        String categoryNameEn,
        String descriptionPreview,
        String areaGovernorate,
        Double distance,
        List<String> attachmentThumbUrls,
        LocalDateTime receivedAt,
        LocalDateTime expiresAt,
        QuoteRequestStatus requestStatus,
        String myResponseStatus
) {}

/** This center's own quote (sealed center view). */
record CenterQuoteResponseDto(
        Long id,
        BigDecimal priceMin,
        BigDecimal priceMax,
        Integer estimatedDurationMinutes,
        String inclusions,
        String message,
        QuoteResponseStatus status,
        LocalDateTime submittedAt,
        LocalDateTime updatedAt
) {}

/** Full request + this center's own quote only (sealed center view). */
record QuoteRequestDetailResponse(
        Long requestId,
        Long categoryId,
        String categoryNameAr,
        String categoryNameEn,
        Long serviceId,
        String description,
        List<String> attachmentUrls,
        String vehicleOrApplianceNote,
        String areaGovernorate,
        Double distance,
        FulfillmentHint fulfillmentHint,
        QuoteRequestStatus requestStatus,
        LocalDateTime expiresAt,
        CenterQuoteResponseDto myResponse
) {}

/** Result of withdrawing the center's quote. */
record WithdrawResultResponse(Long id, QuoteResponseStatus status) {}

/** Result of starting a request-scoped chat. */
record StartChatResponse(Long conversationId) {}
