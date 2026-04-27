package com.maintainance.service_center.analytics;

import com.maintainance.service_center.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/center/summary")
    public ResponseEntity<AnalyticsSummaryResponse> getAnalyticsSummary(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "WEEKLY") String granularity) {

        log.info("Getting analytics summary for user {} from {} to {} granularity {}",
                user.getId(), startDate, endDate, granularity);

        return ResponseEntity.ok(analyticsService.getAnalyticsSummary(user, startDate, endDate, granularity));
    }

    @GetMapping("/center/performance")
    public ResponseEntity<PerformanceSummaryResponse> getPerformanceSummary(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Getting performance summary for user {} from {} to {}", user.getId(), startDate, endDate);

        return ResponseEntity.ok(analyticsService.getPerformanceSummary(user, startDate, endDate));
    }

    @GetMapping("/center/booking-trends")
    public ResponseEntity<List<BookingTrendPoint>> getBookingTrends(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "WEEKLY") String granularity) {

        log.info("Getting booking trends for user {} from {} to {} granularity {}",
                user.getId(), startDate, endDate, granularity);

        return ResponseEntity.ok(analyticsService.getBookingTrends(user, startDate, endDate, granularity));
    }

    @GetMapping("/center/revenue-by-category")
    public ResponseEntity<List<RevenueByCategoryEntry>> getRevenueByCategory(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Getting revenue by category for user {} from {} to {}", user.getId(), startDate, endDate);

        return ResponseEntity.ok(analyticsService.getRevenueByCategory(user, startDate, endDate));
    }

    @GetMapping("/center/satisfaction")
    public ResponseEntity<CustomerSatisfactionResponse> getCustomerSatisfaction(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Getting customer satisfaction for user {} from {} to {}", user.getId(), startDate, endDate);

        return ResponseEntity.ok(analyticsService.getCustomerSatisfaction(user, startDate, endDate));
    }

    @GetMapping("/center/peak-hours")
    public ResponseEntity<List<PeakHourEntry>> getPeakHours(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Getting peak hours for user {} from {} to {}", user.getId(), startDate, endDate);

        return ResponseEntity.ok(analyticsService.getPeakHours(user, startDate, endDate));
    }
}
