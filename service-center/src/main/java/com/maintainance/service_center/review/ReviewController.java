package com.maintainance.service_center.review;

import com.maintainance.service_center.common.PageResponse;
import com.maintainance.service_center.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("reviews")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @Operation(summary = "Create a new review")
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(reviewService.createReview(request, user));
    }

    @GetMapping("/center")
    @Operation(summary = "Get reviews for the authenticated owner's center")
    public ResponseEntity<PageResponse<ReviewResponse>> getMyCenterReviews(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(reviewService.getMyCenterReviews(user, page, size));
    }

    @PostMapping("/{id}/reply")
    @Operation(summary = "Reply to a review as center owner")
    public ResponseEntity<ReviewResponse> replyToReview(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(reviewService.replyToReview(id, body.get("reply"), user));
    }

    @GetMapping("/center/{id}")
    @Operation(summary = "Get reviews for a specific center")
    public ResponseEntity<PageResponse<ReviewResponse>> getCenterReviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(reviewService.getCenterReviews(id, page, size));
    }

    @GetMapping("/my")
    @Operation(summary = "Get current user's reviews")
    public ResponseEntity<PageResponse<ReviewResponse>> getUserReviews(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(reviewService.getUserReviews(user, page, size));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a review")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(reviewService.updateReview(id, request, user));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a review")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        reviewService.deleteReview(id, user);
        return ResponseEntity.noContent().build();
    }
}
