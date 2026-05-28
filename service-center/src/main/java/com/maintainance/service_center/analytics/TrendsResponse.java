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
public class TrendsResponse {
    private List<WeeklyBookingStat> bookingsByWeek;
    private List<WeeklyRatingStat> ratingsByWeek;
    private List<WeeklyRevenueStat> revenueByWeek;
    private List<CategoryMixEntry> categoryMix;
    private List<PeakHourByDayEntry> peakHours;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyBookingStat {
        private String isoWeek;
        private String periodLabel;
        private long total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyRatingStat {
        private String isoWeek;
        private String periodLabel;
        private Double average;
        private long reviewCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyRevenueStat {
        private String isoWeek;
        private String periodLabel;
        private double totalKwd;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryMixEntry {
        private Long categoryId;
        private String categoryNameAr;
        private String categoryNameEn;
        private long bookingCount;
        private double sharePercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeakHourByDayEntry {
        private int dayOfWeek;
        private int hour;
        private long bookingCount;
    }
}
