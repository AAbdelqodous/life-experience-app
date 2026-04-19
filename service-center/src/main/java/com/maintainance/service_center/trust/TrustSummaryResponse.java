package com.maintainance.service_center.trust;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrustSummaryResponse {
    
    private Integer overallScore;
    private List<TrustBadgeResponse> badges;
}