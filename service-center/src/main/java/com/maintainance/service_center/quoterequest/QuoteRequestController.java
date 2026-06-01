package com.maintainance.service_center.quoterequest;

import com.maintainance.service_center.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Spec 009 (customer) + 024 (center). One endpoint set, branched by caller:
 * the owning customer manages the request and accepts; centers respond with sealed quotes.
 */
@RestController
@RequestMapping("quote-requests")
@RequiredArgsConstructor
public class QuoteRequestController {

    private final QuoteRequestService service;

    // ── Customer ──
    @PostMapping
    public ResponseEntity<QuoteRequestResponse> create(
            @AuthenticationPrincipal User caller,
            @Valid @RequestBody CreateQuoteRequestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createRequest(caller, request));
    }

    @GetMapping
    public ResponseEntity<List<QuoteRequestSummaryResponse>> myRequests(
            @AuthenticationPrincipal User caller) {
        return ResponseEntity.ok(service.listMyRequests(caller));
    }

    /** Customer → full view (all responses); center staff → sealed view (own quote only). */
    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(
            @AuthenticationPrincipal User caller,
            @PathVariable Long id) {
        return ResponseEntity.ok(service.getForCaller(caller, id));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<AcceptResultResponse> accept(
            @AuthenticationPrincipal User caller,
            @PathVariable Long id,
            @Valid @RequestBody AcceptQuoteRequestDto request) {
        return ResponseEntity.ok(service.acceptQuote(caller, id, request.quoteId()));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<CancelResultResponse> cancel(
            @AuthenticationPrincipal User caller,
            @PathVariable Long id) {
        return ResponseEntity.ok(service.cancelRequest(caller, id));
    }

    // ── Center ──
    @PostMapping("/{id}/quote")
    public ResponseEntity<CenterQuoteResponseDto> submitQuote(
            @AuthenticationPrincipal User caller,
            @PathVariable Long id,
            @Valid @RequestBody SubmitQuoteRequestDto request) {
        return ResponseEntity.ok(service.submitQuote(caller, id, request));
    }

    @DeleteMapping("/{id}/quote")
    public ResponseEntity<WithdrawResultResponse> withdrawQuote(
            @AuthenticationPrincipal User caller,
            @PathVariable Long id) {
        return ResponseEntity.ok(service.withdrawQuote(caller, id));
    }

    // ── Shared: request-scoped chat (customer ↔ responding center) ──
    @PostMapping("/{id}/chat")
    public ResponseEntity<StartChatResponse> startChat(
            @AuthenticationPrincipal User caller,
            @PathVariable Long id,
            @RequestBody(required = false) StartChatRequest request) {
        Long centerId = request != null ? request.centerId() : null;
        return ResponseEntity.ok(service.startChat(caller, id, centerId));
    }
}
