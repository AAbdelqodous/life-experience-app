package com.maintainance.service_center.staff;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipSummaryResponse {
    private Long centerId;
    private String centerNameAr;
    private String centerNameEn;
    private String centerLogoUrl;
    private CenterRole role;
    private String roleAr;
    private String roleEn;
    private MembershipStatus status;
}
