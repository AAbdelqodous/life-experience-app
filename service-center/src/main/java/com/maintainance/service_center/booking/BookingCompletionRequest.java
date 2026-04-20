package com.maintainance.service_center.booking;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class BookingCompletionRequest {

    private String completionNotes;

    @NotNull(message = "Final cost is required")
    private BigDecimal finalCost;

    private String costNotes;

    private List<String> completionImageUrls;

    /** If null, defaults to PAID. Pass PENDING for pay-later (e.g. invoice). */
    private PaymentStatus paymentStatus;
}
