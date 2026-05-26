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
public class StaffDashboardResponse {
    private long assignedTotal;
    private long assignedActive;
    private long assignedCompleted;
    private long assignedThisWeek;
    private Double averageRatingOnMyBookings;
    private List<StaffPerformanceBoardResponse.ActiveBookingSummary> recentAssignedBookings;
}
