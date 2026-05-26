package com.maintainance.service_center.analytics;

import com.maintainance.service_center.booking.BookingStatus;
import com.maintainance.service_center.staff.CenterRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffPerformanceBoardResponse {
    private List<StaffPerformanceCard> staff;
    private double branchAverageActiveLoad;
    private PerformanceTierConfig config;
    private String generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffPerformanceCard {
        private Long membershipId;
        private Integer userId;
        private String firstName;
        private String lastName;
        private String avatarUrl;
        private CenterRole role;

        private String status; // AVAILABLE, ON_TASK, OVERLOADED, OFFLINE
        private int activeBookingsCount;

        private String tier; // TOP_PERFORMER, STRONG, ON_TRACK, NEEDS_ATTENTION
        private Boolean isNew;
        private Double avgRatingThisMonth;
        private Double avgCompletionTimeMinutes;
        private Integer completedThisMonth;
        private Double onTimeRateThisMonth;

        private String trendDirection; // UP, DOWN, STABLE
        private Double compositeScore;
        private Double previousMonthScore;

        private Boolean isOverloaded;

        private List<ActiveBookingSummary> activeBookings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveBookingSummary {
        private Long bookingId;
        private String customerName;
        private String serviceType;
        private String bookingDate;
        private String bookingTime;
        private BookingStatus bookingStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceTierConfig {
        private double topPerformerMinRating;
        private double topPerformerMinOnTime;
        private int topPerformerMinVolume;
        private double strongMinRating;
        private double strongMinOnTime;
        private double needsAttentionMaxRating;
        private double needsAttentionMinDecline;
        private double needsAttentionMaxOnTime;
        private double trendThreshold;
        private double overloadMultiplier;
    }
}
