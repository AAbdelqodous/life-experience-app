package com.maintainance.service_center.complaint;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintRequest {
    @NotNull(message = "Center ID is required")
    private Long centerId;

    private Long bookingId;

    @NotNull(message = "Complaint type is required")
    private ComplaintType type;

    @NotBlank(message = "Subject is required")
    @Size(min = 5, max = 500, message = "Subject must be between 5 and 500 characters")
    private String subject;

    @NotBlank(message = "Description is required")
    @Size(min = 20, max = 1000, message = "Description must be between 20 and 1000 characters")
    private String description;
}
