package com.maintainance.service_center.booking;

import com.maintainance.service_center.user.User;
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

    @GetMapping("/my")
    public ResponseEntity<List<BookingResponse>> getMyBookings(
            @AuthenticationPrincipal User caller,
            @RequestParam(required = false) BookingStatus status
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

    @GetMapping("/center/{centerId}")
    public ResponseEntity<Page<BookingResponse>> findByCenter(
            @PathVariable Long centerId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(service.findByCenter(centerId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/number/{bookingNumber}")
    public ResponseEntity<BookingResponse> findByBookingNumber(@PathVariable String bookingNumber) {
        return ResponseEntity.ok(service.findByBookingNumber(bookingNumber));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
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
}
