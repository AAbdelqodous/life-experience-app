package com.maintainance.service_center.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSatisfactionResponse {
    
    private Double averageRating;
    private long totalReviews;
    private RatingDistribution distribution;
    private Double trendIndicator;
}