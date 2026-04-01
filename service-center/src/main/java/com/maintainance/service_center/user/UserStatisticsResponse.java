package com.maintainance.service_center.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatisticsResponse {
    
    private Integer totalBookings;
    
    private Integer totalReviews;
    
    private Integer helpfulReviews;
    
    private Integer ownedCenters;
    
    private Integer favorites;
}
