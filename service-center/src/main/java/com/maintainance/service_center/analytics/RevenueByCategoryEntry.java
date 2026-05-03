package com.maintainance.service_center.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueByCategoryEntry {

    private String categoryKey;
    private String categoryNameAr;
    private String categoryNameEn;
    private long bookingCount;
    private double percentage;
    private Double revenue;
    private String color;
}