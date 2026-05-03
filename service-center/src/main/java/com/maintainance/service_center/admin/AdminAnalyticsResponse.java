package com.maintainance.service_center.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAnalyticsResponse {
    private long totalUsers;
    private long totalCustomers;
    private long totalOwners;
    private long newUsersThisMonth;
    private long totalCenters;
    private long approvedCenters;
    private long pendingCenters;
    private long totalBookings;
    private long completedBookings;
    private long cancelledBookings;
    private long bookingsThisMonth;
    private long totalReviews;
    private Double averagePlatformRating;
    private long totalComplaints;
    private long openComplaints;
}
