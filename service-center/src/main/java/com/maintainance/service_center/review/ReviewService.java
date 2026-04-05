package com.maintainance.service_center.review;

import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.common.PageResponse;
import com.maintainance.service_center.user.User;
import com.maintainance.service_center.user.UserType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final MaintenanceCenterRepository centerRepository;

    @Transactional
    public ReviewResponse createReview(ReviewRequest request, User user) {
        log.info("Creating review for center {} by user {}", request.getCenterId(), user.getId());

        MaintenanceCenter center = centerRepository.findById(request.getCenterId())
                .orElseThrow(() -> new EntityNotFoundException("Center not found"));

        // Check if user already reviewed this center
        reviewRepository.findByReviewerIdAndCenterId(user.getId(), request.getCenterId())
                .ifPresent(review -> {
                    throw new IllegalStateException("You have already reviewed this center");
                });

        Review review = Review.builder()
                .reviewer(user)
                .center(center)
                .rating(request.getRating().intValue())
                .comment(request.getComment())
                .build();

        Review savedReview = reviewRepository.save(review);
        log.info("Review created successfully with id {}", savedReview.getId());

        return mapToResponse(savedReview);
    }

    public ReviewStatsResponse getReviewStats(User owner) {
        if (owner == null) {
            throw new IllegalArgumentException("User must be authenticated");
        }
        
        // Verify user is a center owner
        if (owner.getUserType() != UserType.CENTER_OWNER) {
            throw new AccessDeniedException("Only center owners can access review statistics");
        }
        
        MaintenanceCenter center = centerRepository.findByOwnerIdAndIsActiveTrue(
                        owner.getId(), PageRequest.of(0, 1))
                .getContent()
                .stream()
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("No active center found for this owner"));

        long total = reviewRepository.countByCenterId(center.getId());
        Double avg = reviewRepository.calculateAverageRating(center.getId());

        return ReviewStatsResponse.builder()
                .totalReviews(total)
                .averageRating(avg)
                .build();
    }

    public PageResponse<ReviewResponse> getCenterReviews(Long centerId, int page, int size) {
        log.info("Fetching reviews for center {}, page {}, size {}", centerId, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviews = reviewRepository.findByCenterIdOrderByCreatedAtDesc(centerId, pageable);

        return PageResponse.of(reviews.map(this::mapToResponse));
    }

    public PageResponse<ReviewResponse> getUserReviews(User user, int page, int size) {
        log.info("Fetching reviews for user {}, page {}, size {}", user.getId(), page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviews = reviewRepository.findByReviewerId(user.getId(), pageable);

        return PageResponse.of(reviews.map(this::mapToResponse));
    }

    @Transactional
    public ReviewResponse updateReview(Long reviewId, ReviewRequest request, User user) {
        log.info("Updating review {} by user {}", reviewId, user.getId());

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found"));

        // Verify the review belongs to user
        if (!review.getReviewer().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You can only update your own reviews");
        }

        MaintenanceCenter center = centerRepository.findById(request.getCenterId())
                .orElseThrow(() -> new EntityNotFoundException("Center not found"));

        review.setCenter(center);
        review.setRating(request.getRating().intValue());
        review.setComment(request.getComment());

        Review updatedReview = reviewRepository.save(review);
        log.info("Review {} updated successfully", reviewId);

        return mapToResponse(updatedReview);
    }

    @Transactional
    public void deleteReview(Long reviewId, User user) {
        log.info("Deleting review {} by user {}", reviewId, user.getId());

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found"));

        // Verify the review belongs to user
        if (!review.getReviewer().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You can only delete your own reviews");
        }

        reviewRepository.delete(review);
        log.info("Review {} deleted successfully", reviewId);
    }

    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .rating(review.getRating().doubleValue())
                .comment(review.getComment())
                .centerId(review.getCenter().getId())
                .centerNameAr(review.getCenter().getNameAr())
                .centerNameEn(review.getCenter().getNameEn())
                .userFirstname(review.getReviewer().getFirstname())
                .userLastname(review.getReviewer().getLastname())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
