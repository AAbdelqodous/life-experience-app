package com.maintainance.service_center.auth;

import com.maintainance.service_center.user.ApprovalStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AuthenticationResponse {
    private String token;
    private ApprovalStatus approvalStatus;
}
