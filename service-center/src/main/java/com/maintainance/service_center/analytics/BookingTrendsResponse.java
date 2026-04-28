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
public class BookingTrendsResponse {
    
    private String granularity;
    private List<BookingTrendEntry> data;
}
