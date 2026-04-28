package com.maintainance.service_center.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingTrendEntry {
    
    private String periodLabel;
    private String periodStart;
    private long completed;
    private long cancelled;
    private long total;
}
