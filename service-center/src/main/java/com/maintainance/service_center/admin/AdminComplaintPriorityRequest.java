package com.maintainance.service_center.admin;

import com.maintainance.service_center.complaint.ComplaintPriority;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminComplaintPriorityRequest {
    @NotNull(message = "Priority is required")
    private ComplaintPriority priority;
}
