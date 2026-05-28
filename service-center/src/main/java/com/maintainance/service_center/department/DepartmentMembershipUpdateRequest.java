package com.maintainance.service_center.department;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentMembershipUpdateRequest {
    @NotNull(message = "Membership ID is required")
    private Long membershipId;
}
