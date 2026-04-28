package com.maintainance.service_center.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueByCategoryEntry {
    
    private Long categoryId;
    private String categoryNameAr;
    private String categoryNameEn;
    private long completedBookings;
    private BigDecimal revenue;
}