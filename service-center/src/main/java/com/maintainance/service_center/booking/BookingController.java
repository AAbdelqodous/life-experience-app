package com.maintainance.service_center.booking;

import com.maintainance.service_center.progress.BookingMediaResponse;
import com.maintainance.service_center.progress.BookingMediaService;
import com.maintainance.service_center.progress.BookingWorkProgressResponse;
import com.maintainance.service_center.progress.BookingWorkProgressService;
import com.maintainance.service_center.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings")
public class BookingController {

    private final BookingService service;
    private final BookingMediaService bookingMediaService;
    private final BookingWorkProgressService bookingWorkProgressService;

    @GetMapping("/my")
    public ResponseEntity<List<BookingResponse>> getMyBookings(
            @AuthenticationPrincipal User caller,
            @RequestParam BookingStatus status
    ) {
        return ResponseEntity.ok(service.getMyBookings(caller, status));
    }

    @GetMapping
    public ResponseEntity<Page<BookingResponse>> findByCustomer(
            @AuthenticationPrincipal User caller,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(service.findByCustomer(caller, pageable));
    }

    @GetMapping("/center/stats")
    public ResponseEntity<BookingStatsResponse> getCenterStats(
            @AuthenticationPrincipal User caller
    ) {
        return ResponseEntity.ok(service.getCenterStats(caller));
    }

    @GetMapping("/center/{centerId}")
    public ResponseEntity<Page<BookingResponse>> findByCenter(
            @PathVariable Long centerId,
            @AuthenticationPrincipal User caller,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(service.findByCenter(centerId, caller, pageable));
    }

    @GetMapping("/stats")
    public ResponseEntity<BookingStatsResponse> getMyStats(
            @AuthenticationPrincipal User caller
    ) {
        return ResponseEntity.ok(service.getMyStats(caller));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal User caller
    ) {
        return ResponseEntity.ok(service.findById(id, caller));
    }

    @GetMapping("/number/{bookingNumber}")
    public ResponseEntity<BookingResponse> findByBookingNumber(@PathVariable String bookingNumber) {
        return ResponseEntity.ok(service.findByBookingNumber(bookingNumber));
    }

    @PostMapping
    public ResponseEntity<BookingResponse> create(
            @RequestBody @Valid BookingRequest request,
            @AuthenticationPrincipal User caller
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, caller));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookingResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid BookingRequest request,
            @AuthenticationPrincipal User caller
    ) {
        return ResponseEntity.ok(service.update(id, request, caller));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<BookingResponse> confirm(
            @PathVariable Long id,
            @AuthenticationPrincipal User caller
    ) {
        return ResponseEntity.ok(service.confirm(id, caller));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<BookingResponse> startService(
            @PathVariable Long id,
            @AuthenticationPrincipal User caller
    ) {
        return ResponseEntity.ok(service.startService(id, caller));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<BookingResponse> complete(
            @PathVariable Long id,
            @RequestBody @Valid BookingCompletionRequest request,
            @AuthenticationPrincipal User caller
    ) {
        return ResponseEntity.ok(service.complete(id, request, caller));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancel(
            @PathVariable Long id,
            @RequestBody @Valid BookingCancellationRequest request,
            @AuthenticationPrincipal User caller
    ) {
        return ResponseEntity.ok(service.cancel(id, request, caller));
    }

    @GetMapping("/{id}/photos")
    @Operation(summary = "Get booking photos", description = "Get customer-visible photos for a booking")
    public ResponseEntity<List<BookingMediaResponse>> getBookingPhotos(
            @PathVariable Long id,
            @AuthenticationPrincipal User caller
    ) {
        List<BookingMediaResponse> photos = bookingMediaService.getCustomerVisibleMedia(id, caller);
        return ResponseEntity.ok(photos);
    }

    @GetMapping("/{id}/progress")
    @Operation(summary = "Get booking progress", description = "Get work progress for a booking (customer-facing)")
    public ResponseEntity<List<BookingWorkProgressResponse>> getBookingProgress(
            @PathVariable Long id,
            @AuthenticationPrincipal User caller
    ) {
        List<BookingWorkProgressResponse> progress = bookingWorkProgressService.getProgressForCustomer(id, caller);
        return ResponseEntity.ok(progress);
    }
}
