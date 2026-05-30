package com.maintainance.service_center.payment;

import com.maintainance.service_center.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Spec 007 — customer wallet: balance, ledger, and top-up (same gateway flow as a payment). */
@RestController
@RequestMapping("wallet")
@RequiredArgsConstructor
public class WalletController {

    private final PaymentService service;

    @GetMapping
    public ResponseEntity<WalletResponseDto> wallet(@AuthenticationPrincipal User caller) {
        return ResponseEntity.ok(service.getWallet(caller));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<WalletTransactionResponseDto>> transactions(@AuthenticationPrincipal User caller) {
        return ResponseEntity.ok(service.getWalletTransactions(caller));
    }

    @PostMapping("/topup")
    public ResponseEntity<InitiatePaymentResponseDto> topUp(
            @AuthenticationPrincipal User caller, @Valid @RequestBody TopUpRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.topUp(caller, request));
    }
}
