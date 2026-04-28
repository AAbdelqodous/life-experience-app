package com.maintainance.service_center.staff;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteStaffRequest {
    @NotNull(message = "Target email is required")
    @Email(message = "Invalid email format")
    private String targetEmail;

    @NotNull(message = "Target role is required")
    private CenterRole targetRole;
}
