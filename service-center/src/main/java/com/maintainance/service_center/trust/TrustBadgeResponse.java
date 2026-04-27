package com.maintainance.service_center.trust;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TrustBadgeResponse {

    private TrustBadgeType badgeType;
    private boolean isEarned;
    private String earnedAt;
    private String criteriaEn;
    private String criteriaAr;
}
