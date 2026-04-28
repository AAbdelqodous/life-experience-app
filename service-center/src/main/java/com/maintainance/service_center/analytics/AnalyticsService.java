package com.maintainance.service_center.analytics;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.booking.BookingStatus;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.category.ServiceCategory;
import com.maintainance.service_center.review.Review;
import com.maintainance.service_center.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final MaintenanceCenterRepository centerRepository;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public PerformanceSummaryResponse getPerformanceSummary(User user, LocalDate startDate, LocalDate endDate) {
        MaintenanceCenter center = getCenterForOwner(user);
        List<Booking> bookings = bookingRepository.findByCenterIdAndBookingDateBetween(center.getId(), startDate, endDate);

        long totalBookings = bookings.size();
        long completedBookings = bookings.stream().filter(b -> b.getBookingStatus() == BookingStatus.COMPLETED).count();
        long cancelledBookings = bookings.stream().filter(b -> b.getBookingStatus() == BookingStatus.CANCELLED).count();

        double cancellationRate = 0.0;
        if (totalBookings > 0) {
            cancellationRate = ((double) cancelledBookings / totalBookings) * 100;
            cancellationRate = Math.round(cancellationRate * 10.0) / 10.0;
        }

        Double averageRating = bookings.stream()
                .filter(b -> b.getBookingStatus() == BookingStatus.COMPLETED && b.getReview() != null)
                .map(b -> b.getReview().getRating())
                .mapToInt(Integer::intValue)
                .average()
                .orElse(Double.NaN);
        if (averageRating.isNaN()) {
            averageRating = null;
        } else {
            averageRating = Math.round(averageRating * 10.0) / 10.0;
        }

        BigDecimal totalRevenue = bookings.stream()
                .filter(b -> b.getBookingStatus() == BookingStatus.COMPLETED && b.getFinalCost() != null)
                .map(Booking::getFinalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean revenueAvailable = bookings.stream()
                .anyMatch(b -> b.getBookingStatus() == BookingStatus.COMPLETED && b.getFinalCost() != null);

        return PerformanceSummaryResponse.builder()
                .totalBookings(totalBookings)
                .completedBookings(completedBookings)
                .cancelledBookings(cancelledBookings)
                .cancellationRate(cancellationRate)
                .averageRating(averageRating)
                .totalRevenue(totalRevenue)
                .revenueAvailable(revenueAvailable)
                .build();
    }

    @Transactional(readOnly = true)
    public BookingTrendsResponse getBookingTrends(User user, LocalDate startDate, LocalDate endDate, String granularity) {
        MaintenanceCenter center = getCenterForOwner(user);
        List<Booking> bookings = bookingRepository.findByCenterIdAndBookingDateBetween(center.getId(), startDate, endDate);

        List<BookingTrendEntry> data = new ArrayList<>();

        if ("DAILY".equalsIgnoreCase(granularity)) {
            Map<LocalDate, List<Booking>> groupedByDate = bookings.stream()
                    .collect(Collectors.groupingBy(Booking::getBookingDate));

            for (Map.Entry<LocalDate, List<Booking>> entry : groupedByDate.entrySet()) {
                LocalDate date = entry.getKey();
                List<Booking> dayBookings = entry.getValue();

                long total = dayBookings.size();
                long completed = dayBookings.stream().filter(b -> b.getBookingStatus() == BookingStatus.COMPLETED).count();
                long cancelled = dayBookings.stream().filter(b -> b.getBookingStatus() == BookingStatus.CANCELLED).count();

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);
                String periodLabel = date.format(formatter);

                data.add(BookingTrendEntry.builder()
                        .periodLabel(periodLabel)
                        .periodStart(date.toString())
                        .completed(completed)
                        .cancelled(cancelled)
                        .total(total)
                        .build());
            }
        } else {
            Map<LocalDate, List<Booking>> groupedByWeek = bookings.stream()
                    .collect(Collectors.groupingBy(b -> getWeekStart(b.getBookingDate())));

            for (Map.Entry<LocalDate, List<Booking>> entry : groupedByWeek.entrySet()) {
                LocalDate weekStart = entry.getKey();
                List<Booking> weekBookings = entry.getValue();

                long total = weekBookings.size();
                long completed = weekBookings.stream().filter(b -> b.getBookingStatus() == BookingStatus.COMPLETED).count();
                long cancelled = weekBookings.stream().filter(b -> b.getBookingStatus() == BookingStatus.CANCELLED).count();

                String periodLabel = "Week of " + weekStart.getDayOfMonth() + " " + 
                        weekStart.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

                data.add(BookingTrendEntry.builder()
                        .periodLabel(periodLabel)
                        .periodStart(weekStart.toString())
                        .completed(completed)
                        .cancelled(cancelled)
                        .total(total)
                        .build());
            }
        }

        data.sort(Comparator.comparing(BookingTrendEntry::getPeriodStart));

        return BookingTrendsResponse.builder()
                .granularity(granularity.toUpperCase())
                .data(data)
                .build();
    }

    @Transactional(readOnly = true)
    public List<RevenueByCategoryEntry> getRevenueByCategory(User user, LocalDate startDate, LocalDate endDate) {
        MaintenanceCenter center = getCenterForOwner(user);
        List<ServiceCategory> categories = center.getCategories();

        if (categories == null || categories.isEmpty()) {
            return new ArrayList<>();
        }

        List<Booking> bookings = bookingRepository.findByCenterIdAndBookingDateBetween(center.getId(), startDate, endDate);
        List<Booking> completedBookings = bookings.stream()
                .filter(b -> b.getBookingStatus() == BookingStatus.COMPLETED)
                .collect(Collectors.toList());

        long completedCount = completedBookings.size();
        BigDecimal totalRevenue = completedBookings.stream()
                .filter(b -> b.getFinalCost() != null)
                .map(Booking::getFinalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<RevenueByCategoryEntry> entries = new ArrayList<>();
        for (ServiceCategory category : categories) {
            entries.add(RevenueByCategoryEntry.builder()
                    .categoryId(category.getId())
                    .categoryNameAr(category.getNameAr())
                    .categoryNameEn(category.getNameEn())
                    .completedBookings(completedCount)
                    .revenue(totalRevenue)
                    .build());
        }

        return entries;
    }

    @Transactional(readOnly = true)
    public SatisfactionSummaryResponse getSatisfaction(User user, LocalDate startDate, LocalDate endDate) {
        MaintenanceCenter center = getCenterForOwner(user);
        List<Booking> bookings = bookingRepository.findByCenterIdAndBookingDateBetween(center.getId(), startDate, endDate);

        List<Booking> reviewedBookings = bookings.stream()
                .filter(b -> b.getReview() != null)
                .collect(Collectors.toList());

        Double averageRating = null;
        if (!reviewedBookings.isEmpty()) {
            averageRating = reviewedBookings.stream()
                    .map(b -> b.getReview().getRating())
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(Double.NaN);
            if (!averageRating.isNaN()) {
                averageRating = Math.round(averageRating * 10.0) / 10.0;
            }
        }

        Double previousPeriodAverage = null;
        if (startDate != null && endDate != null) {
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
            LocalDate prevEnd = startDate.minusDays(1);
            LocalDate prevStart = prevEnd.minusDays(daysBetween);

            List<Booking> previousBookings = bookingRepository.findByCenterIdAndBookingDateBetween(center.getId(), prevStart, prevEnd);
            List<Booking> previousReviewedBookings = previousBookings.stream()
                    .filter(b -> b.getReview() != null)
                    .collect(Collectors.toList());

            if (!previousReviewedBookings.isEmpty()) {
                previousPeriodAverage = previousReviewedBookings.stream()
                        .map(b -> b.getReview().getRating())
                        .mapToInt(Integer::intValue)
                        .average()
                        .orElse(Double.NaN);
                if (!previousPeriodAverage.isNaN()) {
                    previousPeriodAverage = Math.round(previousPeriodAverage * 10.0) / 10.0;
                }
            }
        }

        long totalReviews = reviewedBookings.size();

        List<RatingDistributionEntry> distribution = new ArrayList<>();
        for (int stars = 5; stars >= 1; stars--) {
            final int starCount = stars;
            long count = reviewedBookings.stream()
                    .filter(b -> Objects.equals(b.getReview().getRating(), starCount))
                    .count();
            distribution.add(RatingDistributionEntry.builder()
                    .stars(stars)
                    .count(count)
                    .build());
        }

        return SatisfactionSummaryResponse.builder()
                .averageRating(averageRating)
                .previousPeriodAverage(previousPeriodAverage)
                .totalReviews(totalReviews)
                .distribution(distribution)
                .build();
    }

    @Transactional(readOnly = true)
    public List<PeakHourEntry> getPeakHours(User user, LocalDate startDate, LocalDate endDate) {
        MaintenanceCenter center = getCenterForOwner(user);
        List<Booking> bookings = bookingRepository.findByCenterIdAndBookingDateBetween(center.getId(), startDate, endDate);

        Map<Integer, Long> hourCounts = bookings.stream()
                .filter(b -> b.getBookingTime() != null)
                .collect(Collectors.groupingBy(b -> b.getBookingTime().getHour(), Collectors.counting()));

        List<PeakHourEntry> entries = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : hourCounts.entrySet()) {
            entries.add(PeakHourEntry.builder()
                    .hour(entry.getKey())
                    .bookingCount(entry.getValue())
                    .build());
        }

        entries.sort(Comparator.comparing(PeakHourEntry::getHour));

        return entries;
    }

    private MaintenanceCenter getCenterForOwner(User user) {
        return centerRepository.findFirstByOwnerId(user.getId())
                .orElseThrow(() -> new RuntimeException("Center not found"));
    }

    private LocalDate getWeekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
