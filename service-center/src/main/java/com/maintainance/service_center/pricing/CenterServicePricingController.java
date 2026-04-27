package com.maintainance.service_center.pricing;

import com.maintainance.service_center.user.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("centers/my/pricing")
@RequiredArgsConstructor
@Tag(name = "Center Service Pricing")
public class CenterServicePricingController {

    private final CenterServicePricingService service;

    @GetMapping
    public ResponseEntity<List<CenterServicePricingResponse>> getMyPricing(
            @AuthenticationPrincipal User owner) {
        return ResponseEntity.ok(service.getMyPricing(owner));
    }

    @PostMapping
    public ResponseEntity<CenterServicePricingResponse> createPricing(
            @AuthenticationPrincipal User owner,
            @RequestBody @Valid CenterServicePricingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createPricing(owner, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CenterServicePricingResponse> updatePricing(
            @AuthenticationPrincipal User owner,
            @PathVariable Long id,
            @RequestBody @Valid CenterServicePricingRequest request) {
        return ResponseEntity.ok(service.updatePricing(owner, id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePricing(
            @AuthenticationPrincipal User owner,
            @PathVariable Long id) {
        service.deletePricing(owner, id);
    }
}
