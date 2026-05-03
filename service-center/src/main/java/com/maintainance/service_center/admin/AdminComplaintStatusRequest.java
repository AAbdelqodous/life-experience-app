package com.maintainance.service_center.admin;

import com.maintainance.service_center.complaint.ComplaintStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminComplaintStatusRequest {
    @NotNull(message = "Status is required")
    private ComplaintStatus status;
}
