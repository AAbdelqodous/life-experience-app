package com.maintainance.service_center.trust;

import com.maintainance.service_center.user.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("centers/my/trust")
@RequiredArgsConstructor
@Tag(name = "Trust Badges")
public class TrustBadgeController {

    private final TrustBadgeService trustBadgeService;

    @GetMapping
    public ResponseEntity<TrustSummaryResponse> getMyTrust(@AuthenticationPrincipal User owner) {
        return ResponseEntity.ok(trustBadgeService.getMyTrust(owner));
    }

    @PostMapping("/evaluate")
    public ResponseEntity<TrustSummaryResponse> evaluateAndGetTrust(@AuthenticationPrincipal User owner) {
        return ResponseEntity.ok(trustBadgeService.evaluateAndGetTrust(owner));
    }
}
