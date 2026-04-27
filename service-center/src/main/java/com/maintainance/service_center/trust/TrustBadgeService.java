package com.maintainance.service_center.trust;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.booking.BookingStatus;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.pricing.CenterServicePricingRepository;
import com.maintainance.service_center.review.ReviewRepository;
import com.maintainance.service_center.user.User;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
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
public class TrustBadgeService {

    private final CenterTrustBadgeRepository centerTrustBadgeRepository;
    private final MaintenanceCenterRepository maintenanceCenterRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final CenterServicePricingRepository centerServicePricingRepository;

    public TrustSummaryResponse getMyTrust(User owner) {
        MaintenanceCenter center = getCenterForOwner(owner);
        return buildSummary(center.getId());
    }

    @Transactional
    public TrustSummaryResponse evaluateAndGetTrust(User owner) {
        MaintenanceCenter center = getCenterForOwner(owner);
        Long centerId = center.getId();

        for (TrustBadgeType badgeType : TrustBadgeType.values()) {
            boolean qualifies = checkBadgeCriteria(centerId, badgeType);
            boolean alreadyEarned = centerTrustBadgeRepository.existsByCenterIdAndBadgeType(centerId, badgeType);

            if (qualifies && !alreadyEarned) {
                CenterTrustBadge badge = new CenterTrustBadge();
                badge.setCenter(center);
                badge.setBadgeType(badgeType);
                centerTrustBadgeRepository.save(badge);
                log.info("Awarded {} badge to center {}", badgeType, centerId);
            } else if (!qualifies && alreadyEarned) {
                centerTrustBadgeRepository.deleteByCenterIdAndBadgeType(centerId, badgeType);
                log.info("Removed {} badge from center {}", badgeType, centerId);
            }
        }

        return buildSummary(centerId);
    }

    private TrustSummaryResponse buildSummary(Long centerId) {
        List<CenterTrustBadge> earnedBadges = centerTrustBadgeRepository.findByCenterId(centerId);
        List<TrustBadgeResponse> badgeResponses = new ArrayList<>();

        for (TrustBadgeType badgeType : TrustBadgeType.values()) {
            boolean isEarned = earnedBadges.stream()
                    .anyMatch(badge -> badge.getBadgeType() == badgeType);

            String earnedAt = isEarned ? earnedBadges.stream()
                    .filter(badge -> badge.getBadgeType() == badgeType)
                    .findFirst()
                    .map(badge -> badge.getEarnedAt() != null ? badge.getEarnedAt().toString() : null)
                    .orElse(null) : null;

            badgeResponses.add(new TrustBadgeResponse(
                    badgeType,
                    isEarned,
                    earnedAt,
                    getCriteriaEn(badgeType),
                    getCriteriaAr(badgeType)
            ));
        }

        return new TrustSummaryResponse(badgeResponses);
    }

    private boolean checkBadgeCriteria(Long centerId, TrustBadgeType badgeType) {
        return switch (badgeType) {
            case FAST_RESPONDER -> checkFastResponder(centerId);
            case TOP_RATED -> checkTopRated(centerId);
            case HIGH_COMPLETION -> checkHighCompletion(centerId);
            case VERIFIED_PRICING -> checkVerifiedPricing(centerId);
        };
    }

    private boolean checkFastResponder(Long centerId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        List<Booking> processedBookings = bookingRepository.findByCenterIdAndStatuses(
                centerId,
                List.of(BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS,
                        BookingStatus.COMPLETED, BookingStatus.CANCELLED)
        ).stream()
                .filter(b -> b.getCreatedAt() != null
                        && b.getCreatedAt().isAfter(thirtyDaysAgo)
                        && b.getUpdatedAt() != null)
                .toList();

        if (processedBookings.size() < 5) {
            return false;
        }

        long totalMinutes = 0;
        for (Booking booking : processedBookings) {
            totalMinutes += Duration.between(booking.getCreatedAt(), booking.getUpdatedAt()).toMinutes();
        }

        return ((double) totalMinutes / processedBookings.size()) < 120;
    }

    private boolean checkTopRated(Long centerId) {
        Integer count = reviewRepository.countByCenterId(centerId);
        if (count == null || count < 10) {
            return false;
        }
        Double avgRating = reviewRepository.calculateAverageRating(centerId);
        return avgRating != null && avgRating >= 4.5;
    }

    private boolean checkHighCompletion(Long centerId) {
        long completed = bookingRepository.countByCenterIdAndStatus(centerId, BookingStatus.COMPLETED);
        long pending = bookingRepository.countByCenterIdAndStatus(centerId, BookingStatus.PENDING);
        long cancelled = bookingRepository.countByCenterIdAndStatus(centerId, BookingStatus.CANCELLED);
        long total = bookingRepository.countByCenterId(centerId) - pending - cancelled;

        if (total < 20) {
            return false;
        }

        return ((double) completed / total) >= 0.90;
    }

    private boolean checkVerifiedPricing(Long centerId) {
        return !centerServicePricingRepository.findByCenterIdAndIsActiveTrue(centerId).isEmpty();
    }

    private String getCriteriaEn(TrustBadgeType badgeType) {
        return switch (badgeType) {
            case FAST_RESPONDER ->
                "Respond to at least 5 bookings within 2 hours on average over the last 30 days";
            case TOP_RATED ->
                "Maintain an average rating of 4.5 or above with at least 10 reviews";
            case HIGH_COMPLETION ->
                "Complete 90% or more of accepted bookings with at least 20 total";
            case VERIFIED_PRICING ->
                "Add at least one active service with pricing to your profile";
        };
    }

    private String getCriteriaAr(TrustBadgeType badgeType) {
        return switch (badgeType) {
            case FAST_RESPONDER ->
                "الرد على 5 حجوزات على الأقل خلال ساعتين في المتوسط خلال آخر 30 يومًا";
            case TOP_RATED ->
                "الحفاظ على تقييم متوسط 4.5 أو أعلى مع 10 تقييمات على الأقل";
            case HIGH_COMPLETION ->
                "إتمام 90% أو أكثر من الحجوزات المقبولة مع 20 حجزًا على الأقل";
            case VERIFIED_PRICING ->
                "إضافة خدمة واحدة على الأقل بسعر محدد إلى ملفك الشخصي";
        };
    }

    private MaintenanceCenter getCenterForOwner(User owner) {
        return maintenanceCenterRepository.findFirstByOwnerId(owner.getId())
                .orElseThrow(() -> new EntityNotFoundException("Maintenance center not found for owner"));
    }
}
