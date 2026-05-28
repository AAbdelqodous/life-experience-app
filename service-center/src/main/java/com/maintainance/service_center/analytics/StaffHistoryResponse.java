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
public class StaffHistoryResponse {
    private Long membershipId;
    private String firstName;
    private String lastName;
    private List<StaffMonthlyMetrics> months;
    private List<StaffPerformanceBoardResponse.ActiveBookingSummary> recentBookings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffMonthlyMetrics {
        private int year;
        private int month;
        private int completedBookings;
        private Double avgRating;
        private Double avgCompletionTimeMinutes;
        private Double onTimeRate;
        private int complaintCount;
    }
}
