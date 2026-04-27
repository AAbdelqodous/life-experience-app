package com.maintainance.service_center.quote;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingQuoteResponse {

    private Long id;
    private Long bookingId;
    private int version;
    private List<QuoteLineItemResponse> lineItems;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private String discountReason;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private Integer estimatedDurationMinutes;
    private String notes;
    private String notesAr;
    private String status;
    private LocalDateTime sentAt;
    private LocalDateTime respondedAt;
    private String responseNotes;
    private String createdAt;

    public static BookingQuoteResponse from(BookingQuote entity) {
        BookingQuoteResponse response = new BookingQuoteResponse();
        response.setId(entity.getId());
        response.setBookingId(entity.getBooking() != null ? entity.getBooking().getId() : null);
        response.setVersion(entity.getVersion());
        
        if (entity.getLineItems() != null) {
            response.setLineItems(entity.getLineItems().stream()
                    .map(item -> {
                        QuoteLineItemResponse itemResponse = new QuoteLineItemResponse();
                        itemResponse.setDescription(item.getDescription());
                        itemResponse.setDescriptionAr(item.getDescriptionAr());
                        itemResponse.setPartsCost(item.getPartsCost());
                        itemResponse.setLaborCost(item.getLaborCost());
                        return itemResponse;
                    })
                    .collect(Collectors.toList()));
        }
        
        response.setSubtotal(entity.getSubtotal());
        response.setDiscountAmount(entity.getDiscountAmount());
        response.setDiscountReason(entity.getDiscountReason());
        response.setTaxAmount(entity.getTaxAmount());
        response.setTotalAmount(entity.getTotalAmount());
        response.setEstimatedDurationMinutes(entity.getEstimatedDurationMinutes());
        response.setNotes(entity.getNotes());
        response.setNotesAr(entity.getNotesAr());
        response.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        response.setSentAt(entity.getSentAt());
        response.setRespondedAt(entity.getRespondedAt());
        response.setResponseNotes(entity.getResponseNotes());
        response.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null);
        return response;
    }
}
