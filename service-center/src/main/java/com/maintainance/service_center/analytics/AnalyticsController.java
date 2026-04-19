package com.maintainance.service_center.analytics;

import com.maintainance.service_center.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {
    
    private final AnalyticsService analyticsService;
    
    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryResponse> getAnalyticsSummary(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "THIS_MONTH") String period) {
        
        log.info("Getting analytics summary for user {} with period {}", user.getId(), period);
        
        AnalyticsSummaryResponse summary = analyticsService.getAnalyticsSummary(user, period);
        
        return ResponseEntity.ok(summary);
    }
    
    @GetMapping("/performance")
    public ResponseEntity<PerformanceSummaryResponse> getPerformanceSummary(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "THIS_MONTH") String period) {
        
        log.info("Getting performance summary for user {} with period {}", user.getId(), period);
        
        PerformanceSummaryResponse performance = analyticsService.getPerformanceSummary(user, period);
        
        return ResponseEntity.ok(performance);
    }
    
    @GetMapping("/trends")
    public ResponseEntity<List<BookingTrendPoint>> getBookingTrends(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "THIS_MONTH") String period) {
        
        log.info("Getting booking trends for user {} with period {}", user.getId(), period);
        
        List<BookingTrendPoint> trends = analyticsService.getBookingTrends(user, period);
        
        return ResponseEntity.ok(trends);
    }
    
    @GetMapping("/revenue-by-category")
    public ResponseEntity<List<RevenueByCategoryEntry>> getRevenueByCategory(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "THIS_MONTH") String period) {
        
        log.info("Getting revenue by category for user {} with period {}", user.getId(), period);
        
        List<RevenueByCategoryEntry> revenue = analyticsService.getRevenueByCategory(user, period);
        
        return ResponseEntity.ok(revenue);
    }
    
    @GetMapping("/satisfaction")
    public ResponseEntity<CustomerSatisfactionResponse> getCustomerSatisfaction(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "THIS_MONTH") String period) {
        
        log.info("Getting customer satisfaction for user {} with period {}", user.getId(), period);
        
        CustomerSatisfactionResponse satisfaction = analyticsService.getCustomerSatisfaction(user, period);
        
        return ResponseEntity.ok(satisfaction);
    }
    
    @GetMapping("/peak-hours")
    public ResponseEntity<List<PeakHourEntry>> getPeakHours(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "THIS_MONTH") String period) {
        
        log.info("Getting peak hours for user {} with period {}", user.getId(), period);
        
        List<PeakHourEntry> peakHours = analyticsService.getPeakHours(user, period);
        
        return ResponseEntity.ok(peakHours);
    }
}