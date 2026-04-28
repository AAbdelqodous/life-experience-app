package com.maintainance.service_center.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceSummaryResponse {
    
    private long totalBookings;
    private long completedBookings;
    private long cancelledBookings;
    private double cancellationRate;
    private Double averageRating;
    private BigDecimal totalRevenue;
    private boolean revenueAvailable;
}