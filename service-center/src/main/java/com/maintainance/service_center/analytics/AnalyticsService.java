package com.maintainance.service_center.analytics;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.booking.BookingStatus;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.review.Review;
import com.maintainance.service_center.review.ReviewRepository;
import com.maintainance.service_center.user.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {
    
    private final MaintenanceCenterRepository centerRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final EntityManager entityManager;
    
    private static final String THIS_WEEK = "THIS_WEEK";
    private static final String THIS_MONTH = "THIS_MONTH";
    private static final String LAST_3_MONTHS = "LAST_3_MONTHS";
    
    public PerformanceSummaryResponse getPerformanceSummary(User owner, String period) {
        LocalDate[] dateRange = getDateRange(period);
        LocalDate startDate = dateRange[0];
        LocalDate endDate = dateRange[1];
        
        MaintenanceCenter center = getCenterForOwner(owner);
        
        // Count total bookings in date range
        long totalBookings = bookingRepository.countByCenterIdAndDateRange(center.getId(), 
                startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        
        // Count completed bookings
        long completedBookings = bookingRepository.countByCenterIdAndStatusAndDateRange(center.getId(), 
                BookingStatus.COMPLETED, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        
        // Count cancelled bookings
        long cancelledBookings = bookingRepository.countByCenterIdAndStatusAndDateRange(center.getId(), 
                BookingStatus.CANCELLED, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        
        // Calculate cancellation rate
        double cancellationRate = 0.0;
        if (totalBookings > 0) {
            cancellationRate = ((double) cancelledBookings / totalBookings) * 100;
            cancellationRate = Math.round(cancellationRate * 100.0) / 100.0;
        }
        
        // Get average rating for the period
        Double averageRating = reviewRepository.getAverageRatingByCenterAndDateRange(
                center.getId(), startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        
        // Calculate total revenue (sum of finalCost for completed bookings)
        BigDecimal totalRevenue = bookingRepository.sumFinalCostByCenterAndDateRange(
                center.getId(), BookingStatus.COMPLETED, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        
        return PerformanceSummaryResponse.builder()
                .totalBookings(totalBookings)
                .completedBookings(completedBookings)
                .cancellationRate(cancellationRate)
                .averageRating(averageRating)
                .totalRevenue(totalRevenue)
                .build();
    }
    
    public List<BookingTrendPoint> getBookingTrends(User owner, String period) {
        LocalDate[] dateRange = getDateRange(period);
        LocalDate startDate = dateRange[0];
        LocalDate endDate = dateRange[1];
        
        MaintenanceCenter center = getCenterForOwner(owner);
        
        List<BookingTrendPoint> trendPoints = new ArrayList<>();
        
        if (THIS_WEEK.equals(period)) {
            // Group by day of week (7 points)
            for (int i = 0; i < 7; i++) {
                LocalDate day = startDate.plusDays(i);
                if (day.isAfter(endDate)) break;
                
                long completedCount = bookingRepository.countByCenterIdAndStatusAndDateRange(
                        center.getId(), BookingStatus.COMPLETED, 
                        day.atStartOfDay(), day.atTime(LocalTime.MAX));
                
                long cancelledCount = bookingRepository.countByCenterIdAndStatusAndDateRange(
                        center.getId(), BookingStatus.CANCELLED, 
                        day.atStartOfDay(), day.atTime(LocalTime.MAX));
                
                trendPoints.add(BookingTrendPoint.builder()
                        .periodLabel(day.toString())
                        .completedCount(completedCount)
                        .cancelledCount(cancelledCount)
                        .build());
            }
        } else {
            // THIS_MONTH or LAST_3_MONTHS: group by ISO week
            LocalDate current = startDate;
            int weekNumber = 1;
            
            while (!current.isAfter(endDate)) {
                LocalDate weekEnd = current.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
                if (weekEnd.isAfter(endDate)) {
                    weekEnd = endDate;
                }
                
                long completedCount = bookingRepository.countByCenterIdAndStatusAndDateRange(
                        center.getId(), BookingStatus.COMPLETED, 
                        current.atStartOfDay(), weekEnd.atTime(LocalTime.MAX));
                
                long cancelledCount = bookingRepository.countByCenterIdAndStatusAndDateRange(
                        center.getId(), BookingStatus.CANCELLED, 
                        current.atStartOfDay(), weekEnd.atTime(LocalTime.MAX));
                
                String weekLabel = getWeekLabel(current);
                
                trendPoints.add(BookingTrendPoint.builder()
                        .periodLabel(weekLabel)
                        .completedCount(completedCount)
                        .cancelledCount(cancelledCount)
                        .build());
                
                current = weekEnd.plusDays(1);
                weekNumber++;
            }
        }
        
        return trendPoints;
    }
    
    public List<RevenueByCategoryEntry> getRevenueByCategory(User owner, String period) {
        LocalDate[] dateRange = getDateRange(period);
        LocalDate startDate = dateRange[0];
        LocalDate endDate = dateRange[1];
        
        MaintenanceCenter center = getCenterForOwner(owner);
        
        // Group completed bookings by service type
        List<Object[]> results = bookingRepository.countCompletedBookingsByServiceTypeAndDateRange(
                center.getId(), BookingStatus.COMPLETED, 
                startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        
        List<RevenueByCategoryEntry> entries = new ArrayList<>();
        for (Object[] result : results) {
            String serviceType = (String) result[0];
            Long count = ((Number) result[1]).longValue();
            BigDecimal revenue = result[2] != null ? (BigDecimal) result[2] : BigDecimal.ZERO;
            
            entries.add(RevenueByCategoryEntry.builder()
                    .serviceType(serviceType)
                    .categoryNameAr(getCategoryNameAr(serviceType))
                    .categoryNameEn(getCategoryNameEn(serviceType))
                    .completedBookings(count)
                    .revenue(revenue)
                    .build());
        }
        
        // Sort by count descending
        entries.sort((a, b) -> Long.compare(b.getCompletedBookings(), a.getCompletedBookings()));
        
        return entries;
    }
    
    public CustomerSatisfactionResponse getCustomerSatisfaction(User owner, String period) {
        LocalDate[] dateRange = getDateRange(period);
        LocalDate startDate = dateRange[0];
        LocalDate endDate = dateRange[1];
        
        MaintenanceCenter center = getCenterForOwner(owner);
        
        // Get average rating for current period
        Double currentAvgRating = reviewRepository.getAverageRatingByCenterAndDateRange(
                center.getId(), startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        
        // Get rating distribution
        long oneStar = reviewRepository.countByCenterAndRatingAndDateRange(
                center.getId(), 1, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        long twoStar = reviewRepository.countByCenterAndRatingAndDateRange(
                center.getId(), 2, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        long threeStar = reviewRepository.countByCenterAndRatingAndDateRange(
                center.getId(), 3, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        long fourStar = reviewRepository.countByCenterAndRatingAndDateRange(
                center.getId(), 4, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        long fiveStar = reviewRepository.countByCenterAndRatingAndDateRange(
                center.getId(), 5, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        
        long totalReviews = oneStar + twoStar + threeStar + fourStar + fiveStar;
        
        RatingDistribution distribution = RatingDistribution.builder()
                .oneStar(oneStar)
                .twoStar(twoStar)
                .threeStar(threeStar)
                .fourStar(fourStar)
                .fiveStar(fiveStar)
                .build();
        
        // Calculate trend indicator (current avg - previous period avg)
        Double trendIndicator = null;
        if (currentAvgRating != null) {
            LocalDate[] previousRange = getPreviousDateRange(period);
            LocalDate prevStartDate = previousRange[0];
            LocalDate prevEndDate = previousRange[1];
            
            Double prevAvgRating = reviewRepository.getAverageRatingByCenterAndDateRange(
                    center.getId(), prevStartDate.atStartOfDay(), prevEndDate.atTime(LocalTime.MAX));
            
            if (prevAvgRating != null) {
                trendIndicator = currentAvgRating - prevAvgRating;
                trendIndicator = Math.round(trendIndicator * 100.0) / 100.0;
            }
        }
        
        return CustomerSatisfactionResponse.builder()
                .averageRating(currentAvgRating)
                .totalReviews(totalReviews)
                .distribution(distribution)
                .trendIndicator(trendIndicator)
                .build();
    }
    
    public List<PeakHourEntry> getPeakHours(User owner, String period) {
        LocalDate[] dateRange = getDateRange(period);
        LocalDate startDate = dateRange[0];
        LocalDate endDate = dateRange[1];
        
        MaintenanceCenter center = centerRepository.findFirstByOwnerId(owner.getId())
                .orElseThrow(() -> new IllegalArgumentException("Center not found for owner"));
        
        List<PeakHourEntry> peakHours = new ArrayList<>();
        
        // Get bookings grouped by hour
        List<Object[]> results = bookingRepository.countBookingsByHourAndDateRange(
                center.getId(), startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        
        // Create a map for easy lookup
        java.util.Map<Integer, Long> hourCounts = new java.util.HashMap<>();
        for (int i = 0; i < 24; i++) {
            hourCounts.put(i, 0L);
        }
        
        for (Object[] result : results) {
            Integer hour = ((Number) result[0]).intValue();
            Long count = ((Number) result[1]).longValue();
            hourCounts.put(hour, count);
        }
        
        // Create entries for all 24 hours (0-23)
        for (int hour = 0; hour < 24; hour++) {
            peakHours.add(PeakHourEntry.builder()
                    .hour(hour)
                    .bookingCount(hourCounts.get(hour))
                    .build());
        }
        
        return peakHours;
    }
    
    public AnalyticsSummaryResponse getAnalyticsSummary(User owner, String period) {
        PerformanceSummaryResponse performance = getPerformanceSummary(owner, period);
        List<BookingTrendPoint> bookingTrends = getBookingTrends(owner, period);
        List<RevenueByCategoryEntry> revenueByCategory = getRevenueByCategory(owner, period);
        CustomerSatisfactionResponse customerSatisfaction = getCustomerSatisfaction(owner, period);
        List<PeakHourEntry> peakHours = getPeakHours(owner, period);
        
        return AnalyticsSummaryResponse.builder()
                .period(period)
                .performance(performance)
                .bookingTrends(bookingTrends)
                .revenueByCategory(revenueByCategory)
                .customerSatisfaction(customerSatisfaction)
                .peakHours(peakHours)
                .build();
    }
    
    private MaintenanceCenter getCenterForOwner(User owner) {
        return centerRepository.findFirstByOwnerId(owner.getId())
                .orElseThrow(() -> new IllegalArgumentException("Center not found for owner"));
    }
    
    private LocalDate[] getDateRange(String period) {
        LocalDate today = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate = today;
        
        if (period == null || THIS_MONTH.equals(period)) {
            startDate = today.withDayOfMonth(1);
        } else if (THIS_WEEK.equals(period)) {
            startDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        } else if (LAST_3_MONTHS.equals(period)) {
            startDate = today.minusMonths(3);
        } else {
            throw new IllegalArgumentException("Invalid period: " + period);
        }
        
        return new LocalDate[]{startDate, endDate};
    }
    
    private LocalDate[] getPreviousDateRange(String period) {
        LocalDate[] currentRange = getDateRange(period);
        LocalDate currentStart = currentRange[0];
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(currentStart, currentRange[1]);
        
        LocalDate prevEnd = currentStart.minusDays(1);
        LocalDate prevStart = prevEnd.minusDays(daysBetween);
        
        return new LocalDate[]{prevStart, prevEnd};
    }
    
    private String getWeekLabel(LocalDate date) {
        int weekNumber = date.get(java.time.temporal.WeekFields.ISO_WEEK_OF_WEEK_BASE_YEAR).get(date);
        String month = date.getMonth().toString().substring(0, 3);
        return month + " W" + weekNumber;
    }
    
    private String getCategoryNameAr(String serviceType) {
        switch (serviceType) {
            case "REPAIR": return "إصلاح";
            case "MAINTENANCE": return "صيانة";
            case "INSPECTION": return "فحص";
            case "INSTALLATION": return "تركيب";
            case "CONSULTATION": return "استشارة";
            case "EMERGENCY": return "طوارئ";
            case "WARRANTY": return "ضمان";
            case "OTHER": return "أخرى";
            default: return serviceType;
        }
    }
    
    private String getCategoryNameEn(String serviceType) {
        return serviceType.substring(0, 1).toUpperCase() + serviceType.substring(1).toLowerCase();
    }
}