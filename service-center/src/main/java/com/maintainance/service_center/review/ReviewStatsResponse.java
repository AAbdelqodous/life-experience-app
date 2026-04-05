package com.maintainance.service_center.review;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewStatsResponse {
    private long totalReviews;
    private Double averageRating;
}
