package com.maintainance.service_center.analytics;

import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.booking.BookingStatus;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.review.ReviewRepository;
import com.maintainance.service_center.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {
    
    private final MaintenanceCenterRepository centerRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    
    public PerformanceSummaryResponse getPerformanceSummary(User owner, LocalDate startDate, LocalDate endDate) {
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
    
    public List<BookingTrendPoint> getBookingTrends(User owner, LocalDate startDate, LocalDate endDate, String granularity) {
        MaintenanceCenter center = getCenterForOwner(owner);

        List<BookingTrendPoint> trendPoints = new ArrayList<>();

        if ("DAILY".equalsIgnoreCase(granularity)) {
            // Group by individual day
            LocalDate day = startDate;
            while (!day.isAfter(endDate)) {
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

                day = day.plusDays(1);
            }
        } else {
            // Default: group by ISO week
            LocalDate current = startDate;
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

                trendPoints.add(BookingTrendPoint.builder()
                        .periodLabel(getWeekLabel(current))
                        .completedCount(completedCount)
                        .cancelledCount(cancelledCount)
                        .build());

                current = weekEnd.plusDays(1);
            }
        }

        return trendPoints;
    }
    
    public List<RevenueByCategoryEntry> getRevenueByCategory(User owner, LocalDate startDate, LocalDate endDate) {
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
    
    public CustomerSatisfactionResponse getCustomerSatisfaction(User owner, LocalDate startDate, LocalDate endDate) {
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
        
        // Calculate trend indicator: compare against the equivalent preceding period
        Double trendIndicator = null;
        if (currentAvgRating != null) {
            long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
            LocalDate prevEnd = startDate.minusDays(1);
            LocalDate prevStart = prevEnd.minusDays(daysBetween);

            Double prevAvgRating = reviewRepository.getAverageRatingByCenterAndDateRange(
                    center.getId(), prevStart.atStartOfDay(), prevEnd.atTime(LocalTime.MAX));

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
    
    public List<PeakHourEntry> getPeakHours(User owner, LocalDate startDate, LocalDate endDate) {
        MaintenanceCenter center = centerRepository.findFirstByOwnerId(owner.getId())
                .orElseThrow(() -> new IllegalArgumentException("Center not found for owner"));
        
        List<PeakHourEntry> peakHours = new ArrayList<>();
        
        // Get bookings grouped by hour
        List<Object[]> results = bookingRepository.countBookingsByHourAndDateRange(
                center.getId(), startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        
        // Create a map for easy lookup
        Map<Integer, Long> hourCounts = new HashMap<>();
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
    
    public AnalyticsSummaryResponse getAnalyticsSummary(User owner, LocalDate startDate, LocalDate endDate, String granularity) {
        PerformanceSummaryResponse performance = getPerformanceSummary(owner, startDate, endDate);
        List<BookingTrendPoint> bookingTrends = getBookingTrends(owner, startDate, endDate, granularity);
        List<RevenueByCategoryEntry> revenueByCategory = getRevenueByCategory(owner, startDate, endDate);
        CustomerSatisfactionResponse customerSatisfaction = getCustomerSatisfaction(owner, startDate, endDate);
        List<PeakHourEntry> peakHours = getPeakHours(owner, startDate, endDate);

        return AnalyticsSummaryResponse.builder()
                .period(startDate + " to " + endDate)
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
    
    private String getWeekLabel(LocalDate date) {
        int weekNumber = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
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