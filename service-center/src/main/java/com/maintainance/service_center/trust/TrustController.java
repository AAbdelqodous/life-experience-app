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
public class TrustController {
    
    private final TrustService service;
    
    @GetMapping
    public ResponseEntity<TrustSummaryResponse> getTrustSummary(
            @AuthenticationPrincipal User caller
    ) {
        return ResponseEntity.ok(service.getTrustSummary(caller));
    }
}