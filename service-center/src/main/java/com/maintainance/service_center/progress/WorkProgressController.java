package com.maintainance.service_center.progress;

import com.maintainance.service_center.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("bookings/{bookingId}")
@RequiredArgsConstructor
public class WorkProgressController {

    private final BookingWorkProgressService workProgressService;
    private final BookingMediaService mediaService;

    @PutMapping("/work-stage")
    public ResponseEntity<Void> updateWorkStage(
            @AuthenticationPrincipal User caller,
            @PathVariable Long bookingId,
            @Valid @RequestBody UpdateWorkStageRequest request) {
        workProgressService.updateWorkStage(bookingId, caller, request);
        return ResponseEntity.ok().build();
    }

    // Spec 009 contract: POST /work-progress is JSON only — photos go via /media.
    @PostMapping("/work-progress")
    public ResponseEntity<BookingWorkProgressResponse> createWorkProgress(
            @AuthenticationPrincipal User caller,
            @PathVariable Long bookingId,
            @Valid @RequestBody CreateWorkProgressRequest request) {
        BookingWorkProgressResponse response = workProgressService.createWorkProgress(
                bookingId, caller, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/work-progress")
    public ResponseEntity<List<BookingWorkProgressResponse>> getWorkProgress(
            @AuthenticationPrincipal User caller,
            @PathVariable Long bookingId) {
        return ResponseEntity.ok(workProgressService.getProgressForOwner(bookingId, caller));
    }

    @PostMapping(value = "/media", consumes = "multipart/form-data")
    public ResponseEntity<BookingMediaResponse> createMedia(
            @AuthenticationPrincipal User caller,
            @PathVariable Long bookingId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) MediaCategory category,
            @RequestParam(value = "caption", required = false) String caption,
            @RequestParam(value = "captionAr", required = false) String captionAr,
            @RequestParam(value = "isVisibleToCustomer", required = false) Boolean isVisibleToCustomer) {
        // Spec 009 contract: category defaults to WORK_IN_PROGRESS.
        MediaCategory resolvedCategory = category != null ? category : MediaCategory.WORK_IN_PROGRESS;
        BookingMediaResponse response = mediaService.createMedia(
                bookingId, caller, file, resolvedCategory, caption, captionAr, isVisibleToCustomer);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/media")
    public ResponseEntity<List<BookingMediaResponse>> getMedia(
            @AuthenticationPrincipal User caller,
            @PathVariable Long bookingId) {
        return ResponseEntity.ok(mediaService.getAllMediaForOwner(bookingId, caller));
    }

    @PostMapping(value = "/customer-media", consumes = "multipart/form-data")
    public ResponseEntity<BookingMediaResponse> uploadCustomerMedia(
            @AuthenticationPrincipal User customer,
            @PathVariable Long bookingId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption,
            @RequestParam(value = "captionAr", required = false) String captionAr) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mediaService.createCustomerMedia(bookingId, customer, file, caption, captionAr));
    }

    @GetMapping("/customer-media")
    public ResponseEntity<List<BookingMediaResponse>> getCustomerMedia(
            @AuthenticationPrincipal User customer,
            @PathVariable Long bookingId) {
        return ResponseEntity.ok(mediaService.getCustomerVisibleMedia(bookingId, customer));
    }
}
