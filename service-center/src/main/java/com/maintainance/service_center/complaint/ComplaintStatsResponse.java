package com.maintainance.service_center.complaint;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ComplaintStatsResponse {
    private long total;
    private long pending;
    private long underReview;
    private long inProgress;
    private long resolved;
    private long closed;
    private long escalated;
}
