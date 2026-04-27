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

    /**
     * Update the work stage of a booking
     * 
     * @param bookingId the booking ID
     * @param owner the authenticated center owner
     * @param request the update request
     */
    @PutMapping("/work-stage")
    public ResponseEntity<Void> updateWorkStage(
            @AuthenticationPrincipal User owner,
            @PathVariable Long bookingId,
            @Valid @RequestBody UpdateWorkStageRequest request) {
        workProgressService.updateWorkStage(bookingId, owner, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Create work progress for a booking
     * 
     * @param bookingId the booking ID
     * @param owner the authenticated center owner
     * @param request the create request
     * @param file optional file upload
     * @return the created work progress response
     */
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

    /**
     * Get all work progress for a booking
     * 
     * @param bookingId the booking ID
     * @param owner the authenticated center owner
     * @return list of work progress responses
     */
    @GetMapping("/work-progress")
    public ResponseEntity<List<BookingWorkProgressResponse>> getWorkProgress(
            @AuthenticationPrincipal User owner,
            @PathVariable Long bookingId) {
        return ResponseEntity.ok(workProgressService.getProgressForOwner(bookingId, owner));
    }

    /**
     * Create media for a booking
     * 
     * @param bookingId the booking ID
     * @param owner the authenticated center owner
     * @param file the uploaded file
     * @param category the media category
     * @param caption the caption
     * @param captionAr the Arabic caption
     * @param isVisibleToCustomer whether the media is visible to the customer
     * @return the created media response
     */
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

    /**
     * Get all media for a booking
     * 
     * @param bookingId the booking ID
     * @param owner the authenticated center owner
     * @return list of media responses
     */
    @GetMapping("/media")
    public ResponseEntity<List<BookingMediaResponse>> getMedia(
            @AuthenticationPrincipal User owner,
            @PathVariable Long bookingId) {
        return ResponseEntity.ok(mediaService.getAllMediaForOwner(bookingId, owner));
    }
}
