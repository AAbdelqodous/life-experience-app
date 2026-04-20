package com.maintainance.service_center.booking;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BookingStatsResponse {
    private long total;
    private long pending;
    private long confirmed;
    private long inProgress;
    private long completed;
    private long cancelled;
    private long noShow;
    private long rescheduled;
    private BigDecimal totalRevenue;
}
