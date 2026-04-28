package com.maintainance.service_center.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SatisfactionSummaryResponse {
    
    private Double averageRating;
    private Double previousPeriodAverage;
    private long totalReviews;
    private List<RatingDistributionEntry> distribution;
}
