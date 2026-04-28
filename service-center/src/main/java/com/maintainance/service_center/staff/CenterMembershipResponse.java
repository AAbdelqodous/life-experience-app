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
public class CenterMembershipResponse {
    private Long id;
    private Integer userId;
    private String userFirstname;
    private String userLastname;
    private String userEmail;
    private CenterRole role;
    private String roleAr;
    private String roleEn;
    private MembershipStatus status;
    private String invitedByName;
    private LocalDateTime activatedAt;
    private Long centerId;
}
