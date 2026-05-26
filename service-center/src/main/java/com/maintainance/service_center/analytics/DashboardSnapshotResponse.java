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
public class DashboardSnapshotResponse {
    private List<PipelineStageData> pipeline;
    private DashboardKpis kpis;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PipelineStageData {
        private String stage;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardKpis {
        private KpiMetric bookingsToday;
        private KpiMetric avgCompletionTimeHours;
        private OnTimeRateMetric onTimeCompletionRate;
        private KpiMetric revenueToday;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KpiMetric {
        private Double value;
        private Double baseline;
        private boolean hasSufficientHistory;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OnTimeRateMetric {
        private Double value;
        private Double baseline;
        private boolean hasSufficientHistory;
        private double target;
    }
}
