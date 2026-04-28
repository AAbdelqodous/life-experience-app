package com.maintainance.service_center.progress;

import com.maintainance.service_center.config.FileStorageService;
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
    private final FileStorageService fileStorageService;

    @PutMapping("/work-stage")
    public ResponseEntity<Void> updateWorkStage(
            @AuthenticationPrincipal User owner,
            @PathVariable Long bookingId,
            @Valid @RequestBody UpdateWorkStageRequest request) {
        workProgressService.updateWorkStage(bookingId, owner, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/work-progress", consumes = "multipart/form-data")
    public ResponseEntity<BookingWorkProgressResponse> createWorkProgress(
            @AuthenticationPrincipal User owner,
            @PathVariable Long bookingId,
            @ModelAttribute CreateWorkProgressRequest request,
            @RequestParam(required = false) MultipartFile file) {
        String fileUrl = null;
        if (file != null && !file.isEmpty()) {
            fileUrl = fileStorageService.storeFile(file);
        }
        BookingWorkProgressResponse response = workProgressService.createWorkProgress(
                bookingId, owner, request, fileUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/work-progress")
    public ResponseEntity<List<BookingWorkProgressResponse>> getWorkProgress(
            @AuthenticationPrincipal User owner,
            @PathVariable Long bookingId) {
        return ResponseEntity.ok(workProgressService.getProgressForOwner(bookingId, owner));
    }

    @PostMapping(value = "/media", consumes = "multipart/form-data")
    public ResponseEntity<BookingMediaResponse> createMedia(
            @AuthenticationPrincipal User owner,
            @PathVariable Long bookingId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") MediaCategory category,
            @RequestParam(value = "caption", required = false) String caption,
            @RequestParam(value = "captionAr", required = false) String captionAr,
            @RequestParam(value = "isVisibleToCustomer", required = false) Boolean isVisibleToCustomer) {
        BookingMediaResponse response = mediaService.createMedia(
                bookingId, owner, file, category, caption, captionAr, isVisibleToCustomer);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/media")
    public ResponseEntity<List<BookingMediaResponse>> getMedia(
            @AuthenticationPrincipal User owner,
            @PathVariable Long bookingId) {
        return ResponseEntity.ok(mediaService.getAllMediaForOwner(bookingId, owner));
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
