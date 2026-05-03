package com.maintainance.service_center.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SatisfactionSummaryResponse {

    private Double averageRating;
    private long totalReviews;
    private long repliedReviews;
    private double replyRate;
    private Map<Integer, Long> ratingDistribution;
}
