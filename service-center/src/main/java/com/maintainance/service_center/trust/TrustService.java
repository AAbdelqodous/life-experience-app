package com.maintainance.service_center.trust;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.booking.BookingStatus;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.pricing.CenterServicePricing;
import com.maintainance.service_center.pricing.CenterServicePricingRepository;
import com.maintainance.service_center.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrustService {
    
    private final MaintenanceCenterRepository centerRepository;
    private final CenterServicePricingRepository pricingRepository;
    private final BookingRepository bookingRepository;
    
    public TrustSummaryResponse getTrustSummary(User owner) {
        MaintenanceCenter center = getCenterForOwner(owner);
        
        List<TrustBadgeResponse> badges = new ArrayList<>();
        
        // Evaluate all badge types
        badges.add(evaluateVerifiedPricing(center));
        badges.add(evaluateQuickResponder(center));
        badges.add(evaluateHighRated(center));
        badges.add(evaluateExperienced(center));
        
        // Calculate overall score
        long earnedCount = badges.stream().filter(TrustBadgeResponse::getIsEarned).count();
        int overallScore = (int) ((earnedCount * 100) / badges.size());
        
        return TrustSummaryResponse.builder()
                .overallScore(overallScore)
                .badges(badges)
                .build();
    }
    
    private TrustBadgeResponse evaluateVerifiedPricing(MaintenanceCenter center) {
        boolean isEarned = pricingRepository.findByCenterIdOrderByCreatedAtDesc(center.getId())
                .stream()
                .anyMatch(CenterServicePricing::getIsActive);
        
        return TrustBadgeResponse.builder()
                .badgeType("VERIFIED_PRICING")
                .isEarned(isEarned)
                .earnedAt(isEarned ? LocalDateTime.now() : null)
                .badgeNameAr("الأسعار الموثقة")
                .badgeNameEn("Verified Pricing")
                .criteriaAr("مركز لديه أسعار موثقة لخدماته")
                .criteriaEn("Center has verified pricing for services")
                .build();
    }
    
    private TrustBadgeResponse evaluateQuickResponder(MaintenanceCenter center) {
        List<Booking> confirmedBookings = bookingRepository.findByCenterIdAndStatuses(
                center.getId(),
                List.of(BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED)
        );
        
        List<Booking> rejectedBookings = bookingRepository.findByCenterIdAndStatuses(
                center.getId(),
                List.of(BookingStatus.CANCELLED)
        );
        
        List<Booking> allProcessedBookings = new ArrayList<>();
        allProcessedBookings.addAll(confirmedBookings);
        allProcessedBookings.addAll(rejectedBookings);
        
        boolean isEarned = false;
        LocalDateTime earnedAt = null;
        
        if (!allProcessedBookings.isEmpty()) {
            // Calculate average response time (time from creation to status change)
            long totalMinutes = 0;
            int count = 0;
            
            for (Booking booking : allProcessedBookings) {
                if (booking.getCreatedAt() != null && booking.getUpdatedAt() != null) {
                    long minutes = Duration.between(booking.getCreatedAt(), booking.getUpdatedAt()).toMinutes();
                    totalMinutes += minutes;
                    count++;
                }
            }
            
            if (count > 0) {
                double averageMinutes = (double) totalMinutes / count;
                isEarned = averageMinutes <= 120; // 2 hours
                if (isEarned) {
                    earnedAt = LocalDateTime.now();
                }
            }
        }
        
        return TrustBadgeResponse.builder()
                .badgeType("QUICK_RESPONDER")
                .isEarned(isEarned)
                .earnedAt(earnedAt)
                .badgeNameAr("مستجيب سريع")
                .badgeNameEn("Quick Responder")
                .criteriaAr("يستجيب للحجوزات في غضون ساعتين في المتوسط")
                .criteriaEn("Responds to bookings within 2 hours on average")
                .build();
    }
    
    private TrustBadgeResponse evaluateHighRated(MaintenanceCenter center) {
        boolean isEarned = center.getAverageRating() != null && 
                          center.getAverageRating().doubleValue() >= 4.5 && 
                          center.getTotalReviews() != null && 
                          center.getTotalReviews() >= 10;
        
        return TrustBadgeResponse.builder()
                .badgeType("HIGH_RATED")
                .isEarned(isEarned)
                .earnedAt(isEarned ? LocalDateTime.now() : null)
                .badgeNameAr("تقييم عالي")
                .badgeNameEn("High Rated")
                .criteriaAr("تقييم متوسط 4.5+ مع 10+ تقييمات")
                .criteriaEn("Average rating of 4.5+ with 10+ reviews")
                .build();
    }
    
    private TrustBadgeResponse evaluateExperienced(MaintenanceCenter center) {
        long completedBookings = bookingRepository.countByCenterIdAndStatus(
                center.getId(),
                BookingStatus.COMPLETED
        );
        
        boolean isEarned = completedBookings >= 50;
        
        return TrustBadgeResponse.builder()
                .badgeType("EXPERIENCED")
                .isEarned(isEarned)
                .earnedAt(isEarned ? LocalDateTime.now() : null)
                .badgeNameAr("متمرس")
                .badgeNameEn("Experienced")
                .criteriaAr("أكمل 50+ حجز")
                .criteriaEn("Completed 50+ bookings")
                .build();
    }
    
    private MaintenanceCenter getCenterForOwner(User owner) {
        return centerRepository.findFirstByOwnerId(owner.getId())
                .orElseThrow(() -> new EntityNotFoundException("Maintenance center not found for owner"));
    }
}