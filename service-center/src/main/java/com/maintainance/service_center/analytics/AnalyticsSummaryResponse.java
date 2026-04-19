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
public class AnalyticsSummaryResponse {
    
    private String period;
    private PerformanceSummaryResponse performance;
    private List<BookingTrendPoint> bookingTrends;
    private List<RevenueByCategoryEntry> revenueByCategory;
    private CustomerSatisfactionResponse customerSatisfaction;
    private List<PeakHourEntry> peakHours;
}