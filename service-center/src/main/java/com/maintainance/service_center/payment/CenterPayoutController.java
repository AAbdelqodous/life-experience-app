package com.maintainance.service_center.payment;

import com.maintainance.service_center.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Spec 023 — center bank payout account + payout requests. */
@RestController
@RequestMapping("centers/my")
@RequiredArgsConstructor
public class CenterPayoutController {

    private final CenterPayoutService service;

    @GetMapping("/payout-account")
    public ResponseEntity<PayoutAccountResponse> getAccount(@AuthenticationPrincipal User caller) {
        return ResponseEntity.ok(service.getAccount(caller));
    }

    @PostMapping("/payout-account")
    public ResponseEntity<PayoutAccountResponse> upsertAccount(
            @AuthenticationPrincipal User caller, @Valid @RequestBody UpsertPayoutAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.upsertAccount(caller, request));
    }

    @GetMapping("/payouts")
    public ResponseEntity<List<PayoutResponse>> listPayouts(@AuthenticationPrincipal User caller) {
        return ResponseEntity.ok(service.listPayouts(caller));
    }

    @PostMapping("/payouts")
    public ResponseEntity<PayoutResponse> requestPayout(
            @AuthenticationPrincipal User caller, @Valid @RequestBody RequestPayoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.requestPayout(caller, request));
    }
}
