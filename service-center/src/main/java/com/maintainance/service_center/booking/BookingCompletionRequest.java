package com.maintainance.service_center.booking;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BookingCompletionRequest {

    private String completionNotes;

    @NotNull(message = "Final cost is required")
    private Double finalCost;

    private String costNotes;

    private List<String> completionImageUrls;
}
