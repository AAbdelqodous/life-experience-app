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
import org.springframework.security.access.prepost.PreAuthorize;
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

    @GetMapping("/assigned")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    @Operation(summary = "Get assigned bookings", description = "Returns bookings assigned to the calling staff member. Requires an active membership.")
    public ResponseEntity<Page<BookingResponse>> getAssignedBookings(
            @AuthenticationPrincipal User caller,
            @RequestParam(required = false) BookingStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(service.findAssignedBookings(caller, status, pageable));
    }

    @GetMapping("/center/stats")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<BookingStatsResponse> getCenterStats(
            @AuthenticationPrincipal User caller
    ) {
        return ResponseEntity.ok(service.getCenterStats(caller));
    }

    @GetMapping("/center/{centerId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Page<BookingResponse>> findByCenter(
            @PathVariable Long centerId,
            @AuthenticationPrincipal User caller,
            @RequestParam(required = false) BookingStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(service.findByCenter(centerId, status, caller, pageable));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
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

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Assign booking to technician", description = "Manually assign (or unassign) a booking to a technician. Requires ASSIGN_TECHNICIAN_MANUAL permission.")
    public ResponseEntity<BookingResponse> assignBooking(
            @PathVariable Long id,
            @RequestBody BookingAssignRequest request,
            @AuthenticationPrincipal User caller
    ) {
        return ResponseEntity.ok(service.assign(id, request.getMembershipId(), caller));
    }

    @GetMapping("/queue")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    @Operation(summary = "Get department booking queue", description = "Returns unassigned claimable bookings for the caller's department(s). Requires TECHNICIAN role.")
    public ResponseEntity<BookingQueueResponse> getQueue(
            @AuthenticationPrincipal User caller,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(service.getQueue(caller, org.springframework.data.domain.PageRequest.of(page, size)));
    }

    @PostMapping("/{id}/claim")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(summary = "Claim a booking", description = "Assigns an unassigned booking to the calling technician atomically.")
    public ResponseEntity<BookingResponse> claimBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal User caller
    ) {
        return ResponseEntity.ok(service.claim(id, caller));
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
