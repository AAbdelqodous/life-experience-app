package com.maintainance.service_center.staff;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationDetailsResponse {
    private Long id;
    private String centerNameAr;
    private String centerNameEn;
    private String centerLogoUrl;
    private String inviterName;
    private CenterRole targetRole;
    private String roleAr;
    private String roleEn;
    private LocalDateTime expiresAt;
    private InvitationStatus status;
}
