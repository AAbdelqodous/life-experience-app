package com.maintainance.service_center.reroute;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for POST /bookings/{id}/reroute (spec 022 §Endpoint 3).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RerouteRequest {

    @NotNull(message = "Target department is required")
    private Long targetDepartmentId;

    @NotNull(message = "Reason is required")
    private RerouteReason reason;

    @Size(max = 500, message = "Note must not exceed 500 characters")
    private String note;
}
