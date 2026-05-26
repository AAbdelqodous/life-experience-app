package com.maintainance.service_center.analytics;

import com.maintainance.service_center.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("analytics")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearerAuth")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/center/summary")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Get performance summary for the center owner")
    public ResponseEntity<PerformanceSummaryResponse> getPerformanceSummary(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Getting performance summary for user {} from {} to {}", user.getId(), startDate, endDate);

        return ResponseEntity.ok(analyticsService.getPerformanceSummary(user, startDate, endDate));
    }

    @GetMapping("/center/booking-trends")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Get booking trends for the center")
    public ResponseEntity<BookingTrendsResponse> getBookingTrends(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String granularity) {

        log.info("Getting booking trends for user {} from {} to {} granularity {}",
                user.getId(), startDate, endDate, granularity);

        return ResponseEntity.ok(analyticsService.getBookingTrends(user, startDate, endDate, granularity));
    }

    @GetMapping("/center/revenue-by-category")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Get revenue breakdown by category")
    public ResponseEntity<List<RevenueByCategoryEntry>> getRevenueByCategory(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Getting revenue by category for user {} from {} to {}", user.getId(), startDate, endDate);

        return ResponseEntity.ok(analyticsService.getRevenueByCategory(user, startDate, endDate));
    }

    @GetMapping("/center/satisfaction")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Get customer satisfaction metrics")
    public ResponseEntity<SatisfactionSummaryResponse> getSatisfaction(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Getting satisfaction for user {} from {} to {}", user.getId(), startDate, endDate);

        return ResponseEntity.ok(analyticsService.getSatisfaction(user, startDate, endDate));
    }

    @GetMapping("/center/peak-hours")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Get peak booking hours")
    public ResponseEntity<List<PeakHourEntry>> getPeakHours(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Getting peak hours for user {} from {} to {}", user.getId(), startDate, endDate);

        return ResponseEntity.ok(analyticsService.getPeakHours(user, startDate, endDate));
    }

    @GetMapping("/center/dashboard-snapshot")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Get real-time dashboard snapshot for center owner")
    public ResponseEntity<DashboardSnapshotResponse> getDashboardSnapshot(
            @AuthenticationPrincipal User user) {
        log.info("Getting dashboard snapshot for user {}", user.getId());
        return ResponseEntity.ok(analyticsService.getDashboardSnapshot(user));
    }

    @GetMapping("/center/staff-performance")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Get staff performance board for center owner")
    public ResponseEntity<StaffPerformanceBoardResponse> getStaffPerformanceBoard(
            @AuthenticationPrincipal User user) {
        log.info("Getting staff performance board for user {}", user.getId());
        return ResponseEntity.ok(analyticsService.getStaffPerformanceBoard(user));
    }

    @GetMapping("/center/staff/{membershipId}/history")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Get individual staff member history")
    public ResponseEntity<StaffHistoryResponse> getStaffHistory(
            @AuthenticationPrincipal User user,
            @PathVariable Long membershipId,
            @RequestParam(defaultValue = "6") int months) {
        log.info("Getting staff history for membership {} by user {}", membershipId, user.getId());
        return ResponseEntity.ok(analyticsService.getStaffHistory(user, membershipId, months));
    }

    @GetMapping("/center/trends")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Get comprehensive trends for center owner")
    public ResponseEntity<TrendsResponse> getTrends(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("Getting trends for user {} from {} to {}", user.getId(), startDate, endDate);
        return ResponseEntity.ok(analyticsService.getTrends(user, startDate, endDate));
    }

    @GetMapping("/staff/dashboard")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(summary = "Get personal dashboard for staff member")
    public ResponseEntity<StaffDashboardResponse> getStaffDashboard(
            @AuthenticationPrincipal User user) {
        log.info("Getting staff dashboard for user {}", user.getId());
        return ResponseEntity.ok(analyticsService.getStaffDashboard(user));
    }
}
