package com.maintainance.service_center.quoterequest;

import com.maintainance.service_center.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Spec 024 — the center's inbox of matching customer quote requests (sealed; gated by
 * RESPOND_TO_QUOTES). Resolved to the caller's active center server-side.
 */
@RestController
@RequestMapping("centers/my/quote-requests")
@RequiredArgsConstructor
public class CenterQuoteInboxController {

    private final QuoteRequestService service;

    @GetMapping
    public ResponseEntity<List<InboxItemResponse>> inbox(@AuthenticationPrincipal User caller) {
        return ResponseEntity.ok(service.getInbox(caller));
    }
}
