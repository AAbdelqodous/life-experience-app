package com.maintainance.service_center.booking;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookingStatsResponse {
    private long total;
    private long pending;
    private long confirmed;
    private long inProgress;
    private long completed;
    private long cancelled;
}
