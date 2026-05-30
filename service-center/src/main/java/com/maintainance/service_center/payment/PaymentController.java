package com.maintainance.service_center.payment;

import com.maintainance.service_center.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Spec 007 — customer payments + escrow. Invoice, initiate (gateway-hosted), status poll,
 * release/dispute, and saved methods. Amounts are KD; the backend is the source of truth.
 */
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService service;

    @GetMapping("/bookings/{bookingId}/invoice")
    public ResponseEntity<BookingInvoiceResponse> invoice(
            @AuthenticationPrincipal User caller, @PathVariable Long bookingId) {
        return ResponseEntity.ok(service.getInvoice(caller, bookingId));
    }

    @PostMapping("/payments")
    public ResponseEntity<InitiatePaymentResponseDto> initiate(
            @AuthenticationPrincipal User caller, @Valid @RequestBody InitiatePaymentRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.initiate(caller, request));
    }

    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<PaymentStatusResponseDto> status(
            @AuthenticationPrincipal User caller, @PathVariable Long paymentId) {
        return ResponseEntity.ok(service.getStatus(caller, paymentId));
    }

    @PostMapping("/bookings/{bookingId}/release")
    public ResponseEntity<ReleaseResponseDto> release(
            @AuthenticationPrincipal User caller, @PathVariable Long bookingId) {
        return ResponseEntity.ok(service.release(caller, bookingId));
    }

    @PostMapping("/bookings/{bookingId}/dispute")
    public ResponseEntity<DisputeResponseDto> dispute(
            @AuthenticationPrincipal User caller,
            @PathVariable Long bookingId,
            @RequestBody(required = false) DisputeRequestDto request) {
        return ResponseEntity.ok(service.dispute(caller, bookingId, request != null ? request.reason() : null));
    }

    @GetMapping("/payments/methods")
    public ResponseEntity<List<SavedMethodResponseDto>> savedMethods(@AuthenticationPrincipal User caller) {
        return ResponseEntity.ok(service.listSavedMethods(caller));
    }

    @DeleteMapping("/payments/methods/{id}")
    public ResponseEntity<Void> deleteSavedMethod(@AuthenticationPrincipal User caller, @PathVariable Long id) {
        service.deleteSavedMethod(caller, id);
        return ResponseEntity.noContent().build();
    }
}
