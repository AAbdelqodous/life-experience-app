package com.maintainance.service_center.analytics;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.booking.BookingStatus;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.category.ServiceCategory;
import com.maintainance.service_center.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private static final Map<String, String> CATEGORY_COLORS = Map.of(
            "CAR",            "#2196F3",
            "ELECTRONICS",    "#9C27B0",
            "HOME_APPLIANCE", "#FF9800",
            "RESTAURANT",     "#F44336",
            "HOTEL",          "#4CAF50",
            "OTHER",          "#607D8B"
    );

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
            cancellationRate = Math.round(((double) cancelledBookings / totalBookings) * 1000.0) / 10.0;
        }

        Double averageRating = bookings.stream()
                .filter(b -> b.getBookingStatus() == BookingStatus.COMPLETED && b.getReview() != null)
                .mapToInt(b -> b.getReview().getRating())
                .average()
                .orElse(Double.NaN);
        if (averageRating.isNaN()) {
            averageRating = null;
        } else {
            averageRating = Math.round(averageRating * 10.0) / 10.0;
        }

        return PerformanceSummaryResponse.builder()
                .totalBookings(totalBookings)
                .completedBookings(completedBookings)
                .cancelledBookings(cancelledBookings)
                .cancellationRate(cancellationRate)
                .averageRating(averageRating)
                .totalRevenue(null)
                .revenueAvailable(false)
                .build();
    }

    @Transactional(readOnly = true)
    public BookingTrendsResponse getBookingTrends(User user, LocalDate startDate, LocalDate endDate, String granularity) {
        MaintenanceCenter center = getCenterForOwner(user);
        List<Booking> bookings = bookingRepository.findByCenterIdAndBookingDateBetween(center.getId(), startDate, endDate);

        List<BookingTrendEntry> data;

        if ("DAILY".equalsIgnoreCase(granularity)) {
            Map<LocalDate, List<Booking>> grouped = bookings.stream()
                    .collect(Collectors.groupingBy(Booking::getBookingDate));

            data = grouped.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> {
                        List<Booking> day = e.getValue();
                        return BookingTrendEntry.builder()
                                .label(e.getKey().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                                .bookings(day.size())
                                .completed(day.stream().filter(b -> b.getBookingStatus() == BookingStatus.COMPLETED).count())
                                .cancelled(day.stream().filter(b -> b.getBookingStatus() == BookingStatus.CANCELLED).count())
                                .build();
                    })
                    .collect(Collectors.toList());
        } else {
            Map<LocalDate, List<Booking>> grouped = bookings.stream()
                    .collect(Collectors.groupingBy(b -> getWeekStart(b.getBookingDate())));

            List<LocalDate> sortedWeeks = grouped.keySet().stream().sorted().collect(Collectors.toList());
            data = new ArrayList<>();
            for (int i = 0; i < sortedWeeks.size(); i++) {
                LocalDate weekStart = sortedWeeks.get(i);
                List<Booking> week = grouped.get(weekStart);
                data.add(BookingTrendEntry.builder()
                        .label("Week " + (i + 1))
                        .bookings(week.size())
                        .completed(week.stream().filter(b -> b.getBookingStatus() == BookingStatus.COMPLETED).count())
                        .cancelled(week.stream().filter(b -> b.getBookingStatus() == BookingStatus.CANCELLED).count())
                        .build());
            }
        }

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
        long totalBookings = bookings.size();

        Map<String, Long> countByServiceType = bookings.stream()
                .filter(b -> b.getServiceType() != null)
                .collect(Collectors.groupingBy(b -> b.getServiceType().name(), Collectors.counting()));

        return categories.stream()
                .map(cat -> {
                    long catCount = countByServiceType.getOrDefault(cat.getCode(), 0L);
                    double pct = totalBookings > 0
                            ? Math.round(((double) catCount / totalBookings) * 1000.0) / 10.0
                            : 0.0;
                    return RevenueByCategoryEntry.builder()
                            .categoryKey(cat.getCode())
                            .categoryNameAr(cat.getNameAr())
                            .categoryNameEn(cat.getNameEn())
                            .bookingCount(catCount)
                            .percentage(pct)
                            .revenue(null)
                            .color(CATEGORY_COLORS.getOrDefault(cat.getCode(), "#607D8B"))
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SatisfactionSummaryResponse getSatisfaction(User user, LocalDate startDate, LocalDate endDate) {
        MaintenanceCenter center = getCenterForOwner(user);
        List<Booking> bookings = bookingRepository.findByCenterIdAndBookingDateBetween(center.getId(), startDate, endDate);

        List<Booking> reviewedBookings = bookings.stream()
                .filter(b -> b.getReview() != null)
                .collect(Collectors.toList());

        long totalReviews = reviewedBookings.size();
        long repliedReviews = reviewedBookings.stream()
                .filter(b -> b.getReview().getCenterResponse() != null && !b.getReview().getCenterResponse().isBlank())
                .count();

        double replyRate = totalReviews > 0
                ? Math.round(((double) repliedReviews / totalReviews) * 1000.0) / 10.0
                : 0.0;

        Double averageRating = null;
        if (totalReviews > 0) {
            double raw = reviewedBookings.stream()
                    .mapToInt(b -> b.getReview().getRating())
                    .average()
                    .orElse(Double.NaN);
            if (!Double.isNaN(raw)) {
                averageRating = Math.round(raw * 10.0) / 10.0;
            }
        }

        Map<Integer, Long> ratingDistribution = new LinkedHashMap<>();
        for (int stars = 1; stars <= 5; stars++) {
            final int s = stars;
            ratingDistribution.put(s, reviewedBookings.stream()
                    .filter(b -> Objects.equals(b.getReview().getRating(), s))
                    .count());
        }

        return SatisfactionSummaryResponse.builder()
                .averageRating(averageRating)
                .totalReviews(totalReviews)
                .repliedReviews(repliedReviews)
                .replyRate(replyRate)
                .ratingDistribution(ratingDistribution)
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
        for (int hour = 0; hour < 24; hour++) {
            entries.add(PeakHourEntry.builder()
                    .hour(hour)
                    .bookingCount(hourCounts.getOrDefault(hour, 0L))
                    .label(formatHourLabel(hour))
                    .build());
        }

        return entries;
    }

    private String formatHourLabel(int hour) {
        if (hour == 0) return "12 AM";
        if (hour < 12) return hour + " AM";
        if (hour == 12) return "12 PM";
        return (hour - 12) + " PM";
    }

    private MaintenanceCenter getCenterForOwner(User user) {
        return centerRepository.findFirstByOwnerId(user.getId())
                .orElseThrow(() -> new RuntimeException("Center not found"));
    }

    private LocalDate getWeekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
