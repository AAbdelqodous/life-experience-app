package com.maintainance.service_center.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.maintainance.service_center.user.ApprovalStatus;
import com.maintainance.service_center.user.UserType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticationResponse {
    private String token;
    private ApprovalStatus approvalStatus;
    private UserType userType;
    private Integer affiliatedCenterId;
}
