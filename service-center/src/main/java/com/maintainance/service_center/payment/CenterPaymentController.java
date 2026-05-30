package com.maintainance.service_center.payment;

import com.maintainance.service_center.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Spec 023 — center-facing payments: earnings, per-booking settlement, mark-complete (release
 * trigger), deposit config, and refunds.
 */
@RestController
@RequiredArgsConstructor
public class CenterPaymentController {

    private final CenterPaymentService service;

    @GetMapping("/centers/my/earnings")
    public ResponseEntity<CenterBalancesResponse> earnings(@AuthenticationPrincipal User caller) {
        return ResponseEntity.ok(service.getEarnings(caller));
    }

    @GetMapping("/bookings/{bookingId}/settlement")
    public ResponseEntity<SettlementResponse> settlement(
            @AuthenticationPrincipal User caller, @PathVariable Long bookingId) {
        return ResponseEntity.ok(service.getSettlement(caller, bookingId));
    }

    // The escrow "mark complete" (release-eligibility) step is now folded into the booking-lifecycle
    // POST /bookings/{id}/complete (see BookingCompletionPaymentListener) — no separate endpoint.

    @PostMapping("/bookings/{bookingId}/refund")
    public ResponseEntity<RefundResponse> refund(
            @AuthenticationPrincipal User caller,
            @PathVariable Long bookingId,
            @Valid @RequestBody RefundRequestDto request) {
        return ResponseEntity.ok(service.refundBooking(caller, bookingId, request));
    }

    @GetMapping("/centers/my/deposit-config")
    public ResponseEntity<DepositConfigResponse> getDeposit(@AuthenticationPrincipal User caller) {
        return ResponseEntity.ok(service.getDepositConfig(caller));
    }

    @PutMapping("/centers/my/deposit-config")
    public ResponseEntity<DepositConfigResponse> updateDeposit(
            @AuthenticationPrincipal User caller, @Valid @RequestBody UpdateDepositConfigRequest request) {
        return ResponseEntity.ok(service.updateDepositConfig(caller, request));
    }
}
