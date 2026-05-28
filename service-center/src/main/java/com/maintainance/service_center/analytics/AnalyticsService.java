package com.maintainance.service_center.analytics;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.booking.BookingStatus;
import com.maintainance.service_center.center.CenterResolverService;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.category.ServiceCategory;
import com.maintainance.service_center.progress.WorkStage;
import com.maintainance.service_center.staff.CenterMembership;
import com.maintainance.service_center.staff.CenterMembershipRepository;
import com.maintainance.service_center.staff.MembershipStatus;
import com.maintainance.service_center.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
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
    private final CenterResolverService centerResolver;
    private final CenterMembershipRepository membershipRepository;

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

    // ── Dashboard Snapshot ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public DashboardSnapshotResponse getDashboardSnapshot(User user) {
        MaintenanceCenter center = getCenterForOwner(user);
        Long centerId = center.getId();

        // Pipeline: count bookings by work stage for active (non-terminal) bookings
        List<Booking> activeBookings = bookingRepository.findByCenterIdAndStatuses(
                centerId,
                List.of(BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS, BookingStatus.RESCHEDULED));

        String[] pipelineStages = {"RECEIVED", "DIAGNOSING", "QUOTE_READY", "IN_PROGRESS", "QUALITY_CHECK", "READY_FOR_PICKUP"};
        Map<String, Long> stageCounts = activeBookings.stream()
                .filter(b -> b.getWorkStage() != null)
                .collect(Collectors.groupingBy(b -> b.getWorkStage().name(), Collectors.counting()));

        List<DashboardSnapshotResponse.PipelineStageData> pipeline = Arrays.stream(pipelineStages)
                .map(s -> DashboardSnapshotResponse.PipelineStageData.builder()
                        .stage(s)
                        .count(stageCounts.getOrDefault(s, 0L))
                        .build())
                .toList();

        // KPIs
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        List<Booking> todayBookings = bookingRepository.findByCenterIdAndBookingDateBetween(centerId, today, today);
        List<Booking> yesterdayBookings = bookingRepository.findByCenterIdAndBookingDateBetween(centerId, yesterday, yesterday);

        double bookingsToday = todayBookings.size();
        double bookingsYesterday = yesterdayBookings.size();
        boolean hasSufficientHistory = bookingsYesterday > 0;

        // Revenue today
        double revenueToday = todayBookings.stream()
                .filter(b -> b.getBookingStatus() == BookingStatus.COMPLETED && b.getFinalCost() != null)
                .mapToDouble(b -> b.getFinalCost().doubleValue())
                .sum();
        double revenueYesterday = yesterdayBookings.stream()
                .filter(b -> b.getBookingStatus() == BookingStatus.COMPLETED && b.getFinalCost() != null)
                .mapToDouble(b -> b.getFinalCost().doubleValue())
                .sum();

        // Avg completion time (hours) for today's completed bookings
        List<Booking> completedToday = todayBookings.stream()
                .filter(b -> b.getBookingStatus() == BookingStatus.COMPLETED && b.getCompletedAt() != null && b.getCreatedAt() != null)
                .toList();
        Double avgCompletionHours = completedToday.isEmpty() ? null :
                completedToday.stream()
                        .mapToDouble(b -> ChronoUnit.MINUTES.between(b.getCreatedAt(), b.getCompletedAt()) / 60.0)
                        .average().orElse(0);

        List<Booking> completedYesterday = yesterdayBookings.stream()
                .filter(b -> b.getBookingStatus() == BookingStatus.COMPLETED && b.getCompletedAt() != null && b.getCreatedAt() != null)
                .toList();
        Double avgCompletionYesterday = completedYesterday.isEmpty() ? null :
                completedYesterday.stream()
                        .mapToDouble(b -> ChronoUnit.MINUTES.between(b.getCreatedAt(), b.getCompletedAt()) / 60.0)
                        .average().orElse(0);

        DashboardSnapshotResponse.DashboardKpis kpis = DashboardSnapshotResponse.DashboardKpis.builder()
                .bookingsToday(DashboardSnapshotResponse.KpiMetric.builder()
                        .value(bookingsToday)
                        .baseline(bookingsYesterday > 0 ? bookingsYesterday : null)
                        .hasSufficientHistory(hasSufficientHistory)
                        .build())
                .avgCompletionTimeHours(DashboardSnapshotResponse.KpiMetric.builder()
                        .value(avgCompletionHours)
                        .baseline(avgCompletionYesterday)
                        .hasSufficientHistory(avgCompletionYesterday != null)
                        .build())
                .onTimeCompletionRate(DashboardSnapshotResponse.OnTimeRateMetric.builder()
                        .value(null) // requires estimated vs actual comparison — placeholder
                        .baseline(null)
                        .hasSufficientHistory(false)
                        .target(90.0)
                        .build())
                .revenueToday(DashboardSnapshotResponse.KpiMetric.builder()
                        .value(revenueToday)
                        .baseline(revenueYesterday > 0 ? revenueYesterday : null)
                        .hasSufficientHistory(revenueYesterday > 0)
                        .build())
                .build();

        return DashboardSnapshotResponse.builder()
                .pipeline(pipeline)
                .kpis(kpis)
                .build();
    }

    // ── Staff Performance Board ───────────────────────────────────

    @Transactional(readOnly = true)
    public StaffPerformanceBoardResponse getStaffPerformanceBoard(User user) {
        MaintenanceCenter center = getCenterForOwner(user);
        Long centerId = center.getId();

        List<CenterMembership> activeStaff = membershipRepository
                .findByCenterIdAndStatus(centerId, MembershipStatus.ACTIVE, PageRequest.of(0, 200))
                .getContent();

        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate monthEnd = LocalDate.now();

        List<StaffPerformanceBoardResponse.StaffPerformanceCard> cards = new ArrayList<>();
        double totalActiveLoad = 0;

        for (CenterMembership m : activeStaff) {
            List<Booking> assignedBookings = bookingRepository
                    .findByAssignedMembershipId(m.getId(), PageRequest.of(0, 1000))
                    .getContent();

            List<Booking> activeAssigned = assignedBookings.stream()
                    .filter(b -> b.getBookingStatus() != BookingStatus.COMPLETED
                              && b.getBookingStatus() != BookingStatus.CANCELLED)
                    .toList();

            List<Booking> completedThisMonth = assignedBookings.stream()
                    .filter(b -> b.getBookingStatus() == BookingStatus.COMPLETED
                              && b.getCompletedAt() != null
                              && !b.getCompletedAt().toLocalDate().isBefore(monthStart))
                    .toList();

            int activeCount = activeAssigned.size();
            totalActiveLoad += activeCount;

            Double avgRating = completedThisMonth.stream()
                    .filter(b -> b.getReview() != null)
                    .mapToInt(b -> b.getReview().getRating())
                    .average().stream().findFirst().orElse(Double.NaN);
            if (avgRating.isNaN()) avgRating = null;

            Double avgCompletionMin = completedThisMonth.stream()
                    .filter(b -> b.getCompletedAt() != null && b.getCreatedAt() != null)
                    .mapToDouble(b -> ChronoUnit.MINUTES.between(b.getCreatedAt(), b.getCompletedAt()))
                    .average().stream().findFirst().orElse(Double.NaN);
            if (avgCompletionMin.isNaN()) avgCompletionMin = null;

            String status = activeCount == 0 ? "AVAILABLE" : activeCount >= 5 ? "OVERLOADED" : "ON_TASK";
            boolean isOverloaded = activeCount >= 5;

            // Tier classification
            String tier = "ON_TRACK";
            if (avgRating != null && avgRating >= 4.5 && completedThisMonth.size() >= 10) {
                tier = "TOP_PERFORMER";
            } else if (avgRating != null && avgRating >= 4.0) {
                tier = "STRONG";
            } else if (avgRating != null && avgRating < 3.0) {
                tier = "NEEDS_ATTENTION";
            }

            List<StaffPerformanceBoardResponse.ActiveBookingSummary> activeSummaries = activeAssigned.stream()
                    .limit(5)
                    .map(b -> StaffPerformanceBoardResponse.ActiveBookingSummary.builder()
                            .bookingId(b.getId())
                            .customerName(b.getCustomer().fullName())
                            .serviceType(b.getServiceType() != null ? b.getServiceType().name() :
                                    (b.getService() != null ? b.getService().getNameEn() : null))
                            .bookingDate(b.getBookingDate() != null ? b.getBookingDate().toString() : null)
                            .bookingTime(b.getBookingTime() != null ? b.getBookingTime().toString() : null)
                            .bookingStatus(b.getBookingStatus())
                            .build())
                    .toList();

            cards.add(StaffPerformanceBoardResponse.StaffPerformanceCard.builder()
                    .membershipId(m.getId())
                    .userId(m.getUser().getId())
                    .firstName(m.getUser().getFirstname())
                    .lastName(m.getUser().getLastname())
                    .role(m.getRole())
                    .status(status)
                    .activeBookingsCount(activeCount)
                    .tier(tier)
                    .isNew(m.getCreatedAt() != null && m.getCreatedAt().isAfter(LocalDateTime.now().minusDays(30)))
                    .avgRatingThisMonth(avgRating)
                    .avgCompletionTimeMinutes(avgCompletionMin)
                    .completedThisMonth(completedThisMonth.size())
                    .trendDirection("STABLE")
                    .isOverloaded(isOverloaded)
                    .activeBookings(activeSummaries)
                    .build());
        }

        double branchAvg = activeStaff.isEmpty() ? 0 : totalActiveLoad / activeStaff.size();

        return StaffPerformanceBoardResponse.builder()
                .staff(cards)
                .branchAverageActiveLoad(Math.round(branchAvg * 100.0) / 100.0)
                .config(StaffPerformanceBoardResponse.PerformanceTierConfig.builder()
                        .topPerformerMinRating(4.5)
                        .topPerformerMinOnTime(90)
                        .topPerformerMinVolume(10)
                        .strongMinRating(4.0)
                        .strongMinOnTime(80)
                        .needsAttentionMaxRating(3.0)
                        .needsAttentionMinDecline(20)
                        .needsAttentionMaxOnTime(60)
                        .trendThreshold(10)
                        .overloadMultiplier(1.5)
                        .build())
                .generatedAt(LocalDateTime.now().toString())
                .build();
    }

    // ── Staff Monthly History ─────────────────────────────────────

    @Transactional(readOnly = true)
    public StaffHistoryResponse getStaffHistory(User user, Long membershipId, int months) {
        MaintenanceCenter center = getCenterForOwner(user);

        CenterMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new EntityNotFoundException("Membership not found"));

        if (!membership.getCenter().getId().equals(center.getId())) {
            throw new IllegalArgumentException("Staff member does not belong to your center");
        }

        List<Booking> allAssigned = bookingRepository
                .findByAssignedMembershipId(membershipId, PageRequest.of(0, 5000))
                .getContent();

        LocalDate now = LocalDate.now();
        List<StaffHistoryResponse.StaffMonthlyMetrics> monthlyMetrics = new ArrayList<>();

        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthStart = now.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

            List<Booking> monthCompleted = allAssigned.stream()
                    .filter(b -> b.getBookingStatus() == BookingStatus.COMPLETED
                              && b.getCompletedAt() != null
                              && !b.getCompletedAt().toLocalDate().isBefore(monthStart)
                              && !b.getCompletedAt().toLocalDate().isAfter(monthEnd))
                    .toList();

            Double avgRating = monthCompleted.stream()
                    .filter(b -> b.getReview() != null)
                    .mapToInt(b -> b.getReview().getRating())
                    .average().stream().findFirst().orElse(Double.NaN);
            if (avgRating.isNaN()) avgRating = null;

            Double avgCompletion = monthCompleted.stream()
                    .filter(b -> b.getCompletedAt() != null && b.getCreatedAt() != null)
                    .mapToDouble(b -> ChronoUnit.MINUTES.between(b.getCreatedAt(), b.getCompletedAt()))
                    .average().stream().findFirst().orElse(Double.NaN);
            if (avgCompletion.isNaN()) avgCompletion = null;

            monthlyMetrics.add(StaffHistoryResponse.StaffMonthlyMetrics.builder()
                    .year(monthStart.getYear())
                    .month(monthStart.getMonthValue())
                    .completedBookings(monthCompleted.size())
                    .avgRating(avgRating)
                    .avgCompletionTimeMinutes(avgCompletion)
                    .onTimeRate(null) // requires estimated time tracking
                    .complaintCount(0) // requires complaint-to-booking linkage
                    .build());
        }

        // Recent active bookings
        List<StaffPerformanceBoardResponse.ActiveBookingSummary> recent = allAssigned.stream()
                .filter(b -> b.getBookingStatus() != BookingStatus.CANCELLED)
                .sorted(Comparator.comparing(Booking::getCreatedAt).reversed())
                .limit(10)
                .map(b -> StaffPerformanceBoardResponse.ActiveBookingSummary.builder()
                        .bookingId(b.getId())
                        .customerName(b.getCustomer().fullName())
                        .serviceType(b.getServiceType() != null ? b.getServiceType().name() :
                                (b.getService() != null ? b.getService().getNameEn() : null))
                        .bookingDate(b.getBookingDate() != null ? b.getBookingDate().toString() : null)
                        .bookingTime(b.getBookingTime() != null ? b.getBookingTime().toString() : null)
                        .bookingStatus(b.getBookingStatus())
                        .build())
                .toList();

        return StaffHistoryResponse.builder()
                .membershipId(membershipId)
                .firstName(membership.getUser().getFirstname())
                .lastName(membership.getUser().getLastname())
                .months(monthlyMetrics)
                .recentBookings(recent)
                .build();
    }

    // ── Trends ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TrendsResponse getTrends(User user, LocalDate startDate, LocalDate endDate) {
        MaintenanceCenter center = getCenterForOwner(user);
        List<Booking> bookings = bookingRepository.findByCenterIdAndBookingDateBetween(center.getId(), startDate, endDate);

        // Bookings by week
        Map<String, List<Booking>> byWeek = bookings.stream()
                .filter(b -> b.getBookingDate() != null)
                .collect(Collectors.groupingBy(b -> isoWeekKey(b.getBookingDate())));

        List<String> sortedWeeks = new ArrayList<>(byWeek.keySet());
        Collections.sort(sortedWeeks);

        List<TrendsResponse.WeeklyBookingStat> bookingsByWeek = sortedWeeks.stream()
                .map(w -> TrendsResponse.WeeklyBookingStat.builder()
                        .isoWeek(w)
                        .periodLabel(w)
                        .total(byWeek.get(w).size())
                        .build())
                .toList();

        // Ratings by week
        List<TrendsResponse.WeeklyRatingStat> ratingsByWeek = sortedWeeks.stream()
                .map(w -> {
                    List<Booking> weekBookings = byWeek.get(w);
                    List<Integer> ratings = weekBookings.stream()
                            .filter(b -> b.getReview() != null)
                            .map(b -> b.getReview().getRating())
                            .toList();
                    Double avg = ratings.isEmpty() ? null :
                            Math.round(ratings.stream().mapToInt(Integer::intValue).average().orElse(0) * 10.0) / 10.0;
                    return TrendsResponse.WeeklyRatingStat.builder()
                            .isoWeek(w)
                            .periodLabel(w)
                            .average(avg)
                            .reviewCount(ratings.size())
                            .build();
                })
                .toList();

        // Revenue by week
        List<TrendsResponse.WeeklyRevenueStat> revenueByWeek = sortedWeeks.stream()
                .map(w -> {
                    double total = byWeek.get(w).stream()
                            .filter(b -> b.getBookingStatus() == BookingStatus.COMPLETED && b.getFinalCost() != null)
                            .mapToDouble(b -> b.getFinalCost().doubleValue())
                            .sum();
                    return TrendsResponse.WeeklyRevenueStat.builder()
                            .isoWeek(w)
                            .periodLabel(w)
                            .totalKwd(Math.round(total * 1000.0) / 1000.0)
                            .build();
                })
                .toList();

        // Category mix
        long totalBookings = bookings.size();
        Map<ServiceCategory, Long> catCounts = bookings.stream()
                .filter(b -> b.getCategory() != null)
                .collect(Collectors.groupingBy(Booking::getCategory, Collectors.counting()));

        List<TrendsResponse.CategoryMixEntry> categoryMix = catCounts.entrySet().stream()
                .map(e -> TrendsResponse.CategoryMixEntry.builder()
                        .categoryId(e.getKey().getId())
                        .categoryNameAr(e.getKey().getNameAr())
                        .categoryNameEn(e.getKey().getNameEn())
                        .bookingCount(e.getValue())
                        .sharePercent(totalBookings > 0 ?
                                Math.round(((double) e.getValue() / totalBookings) * 1000.0) / 10.0 : 0)
                        .build())
                .toList();

        // Peak hours by day of week
        List<TrendsResponse.PeakHourByDayEntry> peakHours = bookings.stream()
                .filter(b -> b.getBookingDate() != null && b.getBookingTime() != null)
                .collect(Collectors.groupingBy(
                        b -> b.getBookingDate().getDayOfWeek().getValue() + "-" + b.getBookingTime().getHour(),
                        Collectors.counting()))
                .entrySet().stream()
                .map(e -> {
                    String[] parts = e.getKey().split("-");
                    return TrendsResponse.PeakHourByDayEntry.builder()
                            .dayOfWeek(Integer.parseInt(parts[0]))
                            .hour(Integer.parseInt(parts[1]))
                            .bookingCount(e.getValue())
                            .build();
                })
                .toList();

        return TrendsResponse.builder()
                .bookingsByWeek(bookingsByWeek)
                .ratingsByWeek(ratingsByWeek)
                .revenueByWeek(revenueByWeek)
                .categoryMix(categoryMix)
                .peakHours(peakHours)
                .build();
    }

    private String isoWeekKey(LocalDate date) {
        int year = date.get(IsoFields.WEEK_BASED_YEAR);
        int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return String.format("%d-W%02d", year, week);
    }

    private String formatHourLabel(int hour) {
        if (hour == 0) return "12 AM";
        if (hour < 12) return hour + " AM";
        if (hour == 12) return "12 PM";
        return (hour - 12) + " PM";
    }

    @Transactional(readOnly = true)
    public StaffDashboardResponse getStaffDashboard(User user) {
        CenterMembership membership = membershipRepository
                .findByUserIdAndStatus(user.getId(), MembershipStatus.ACTIVE)
                .stream()
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("No active center membership found"));

        List<Booking> allAssigned = bookingRepository
                .findByAssignedMembershipId(membership.getId(), PageRequest.of(0, 1000))
                .getContent();

        long assignedTotal = allAssigned.size();
        long assignedActive = allAssigned.stream()
                .filter(b -> b.getBookingStatus() != BookingStatus.COMPLETED
                          && b.getBookingStatus() != BookingStatus.CANCELLED)
                .count();
        long assignedCompleted = allAssigned.stream()
                .filter(b -> b.getBookingStatus() == BookingStatus.COMPLETED)
                .count();

        LocalDate weekStart = LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        long assignedThisWeek = allAssigned.stream()
                .filter(b -> b.getBookingDate() != null && !b.getBookingDate().isBefore(weekStart))
                .count();

        Double avgRating = allAssigned.stream()
                .filter(b -> b.getBookingStatus() == BookingStatus.COMPLETED && b.getReview() != null)
                .mapToInt(b -> b.getReview().getRating())
                .average().stream().findFirst().orElse(Double.NaN);
        if (avgRating.isNaN()) avgRating = null;
        else avgRating = Math.round(avgRating * 10.0) / 10.0;

        List<StaffPerformanceBoardResponse.ActiveBookingSummary> recentBookings = allAssigned.stream()
                .filter(b -> b.getBookingStatus() != BookingStatus.COMPLETED
                          && b.getBookingStatus() != BookingStatus.CANCELLED)
                .sorted(Comparator.comparing(Booking::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(b -> StaffPerformanceBoardResponse.ActiveBookingSummary.builder()
                        .bookingId(b.getId())
                        .customerName(b.getCustomer().fullName())
                        .serviceType(b.getServiceType() != null ? b.getServiceType().name() :
                                (b.getService() != null ? b.getService().getNameEn() : null))
                        .bookingDate(b.getBookingDate() != null ? b.getBookingDate().toString() : null)
                        .bookingTime(b.getBookingTime() != null ? b.getBookingTime().toString() : null)
                        .bookingStatus(b.getBookingStatus())
                        .build())
                .toList();

        return StaffDashboardResponse.builder()
                .assignedTotal(assignedTotal)
                .assignedActive(assignedActive)
                .assignedCompleted(assignedCompleted)
                .assignedThisWeek(assignedThisWeek)
                .averageRatingOnMyBookings(avgRating)
                .recentAssignedBookings(recentBookings)
                .build();
    }

    private MaintenanceCenter getCenterForOwner(User user) {
        return centerResolver.resolveCenter(user);
    }

    private LocalDate getWeekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
