package com.maintainance.service_center.admin;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminRejectRequest {

    @Size(max = 500, message = "Reason must be at most 500 characters")
    private String reason;
}
