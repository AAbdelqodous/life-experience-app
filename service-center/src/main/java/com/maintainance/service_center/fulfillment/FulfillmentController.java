package com.maintainance.service_center.fulfillment;

import com.maintainance.service_center.booking.ServiceAddress;
import com.maintainance.service_center.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Spec 008 — customer-facing fulfillment: center capability (modes/area/fees), per-booking logistics,
 * re-choice after a decline, and saved service addresses.
 */
@RestController
@RequiredArgsConstructor
public class FulfillmentController {

    private final FulfillmentService service;

    @GetMapping("/centers/{centerId}/fulfillment")
    public ResponseEntity<CapabilityResponse> capability(
            @PathVariable Long centerId, @RequestParam(required = false) Long serviceId) {
        return ResponseEntity.ok(service.getCapability(centerId, serviceId));
    }

    // ── Owner-authored capability (modes / service area / fees) ──
    @GetMapping("/centers/my/fulfillment")
    public ResponseEntity<CapabilityResponse> myCapability(@AuthenticationPrincipal User caller) {
        return ResponseEntity.ok(service.getMyCapability(caller));
    }

    @PutMapping("/centers/my/fulfillment")
    public ResponseEntity<CapabilityResponse> updateCapability(
            @AuthenticationPrincipal User caller, @RequestBody UpdateCapabilityRequest request) {
        return ResponseEntity.ok(service.updateCapability(caller, request));
    }

    @GetMapping("/bookings/{bookingId}/logistics")
    public ResponseEntity<LogisticsResponse> logistics(
            @AuthenticationPrincipal User caller, @PathVariable Long bookingId) {
        return ResponseEntity.ok(service.getLogistics(caller, bookingId));
    }

    @PostMapping("/bookings/{bookingId}/fulfillment/re-choose")
    public ResponseEntity<LogisticsResponse> reChoose(
            @AuthenticationPrincipal User caller, @PathVariable Long bookingId,
            @RequestBody ReChooseRequest request) {
        return ResponseEntity.ok(service.reChoose(caller, bookingId, request.mode()));
    }

    /** Center-side: advance the booking's pickup/at-home logistics leg (UPDATE_WORK_STAGE permission). */
    @PostMapping("/centers/my/bookings/{bookingId}/logistics/advance")
    public ResponseEntity<LogisticsResponse> advanceLogistics(
            @AuthenticationPrincipal User caller, @PathVariable Long bookingId,
            @RequestBody(required = false) AdvanceLogisticsRequest request) {
        return ResponseEntity.ok(service.advanceLogistics(
                caller, bookingId, request != null ? request.targetState() : null));
    }

    // ── Saved addresses ──
    @GetMapping("/me/addresses")
    public ResponseEntity<List<SavedAddressView>> listAddresses(@AuthenticationPrincipal User caller) {
        return ResponseEntity.ok(service.listAddresses(caller));
    }

    @PostMapping("/me/addresses")
    public ResponseEntity<SavedAddressView> createAddress(
            @AuthenticationPrincipal User caller, @RequestBody ServiceAddress address) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createAddress(caller, address));
    }

    @PutMapping("/me/addresses/{id}")
    public ResponseEntity<SavedAddressView> updateAddress(
            @AuthenticationPrincipal User caller, @PathVariable Long id, @RequestBody ServiceAddress address) {
        return ResponseEntity.ok(service.updateAddress(caller, id, address));
    }

    @DeleteMapping("/me/addresses/{id}")
    public ResponseEntity<Void> deleteAddress(@AuthenticationPrincipal User caller, @PathVariable Long id) {
        service.deleteAddress(caller, id);
        return ResponseEntity.noContent().build();
    }
}
