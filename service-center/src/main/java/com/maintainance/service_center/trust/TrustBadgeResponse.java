package com.maintainance.service_center.trust;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrustBadgeResponse {
    
    private String badgeType;
    private Boolean isEarned;
    private LocalDateTime earnedAt;
    private String badgeNameAr;
    private String badgeNameEn;
    private String criteriaAr;
    private String criteriaEn;
}