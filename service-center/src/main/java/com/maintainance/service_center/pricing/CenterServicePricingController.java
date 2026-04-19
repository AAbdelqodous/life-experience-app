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
            @AuthenticationPrincipal User caller
    ) {
        return ResponseEntity.ok(service.getMyPricing(caller));
    }
    
    @PostMapping
    public ResponseEntity<CenterServicePricingResponse> createPricing(
            @RequestBody @Valid CreatePricingRequest request,
            @AuthenticationPrincipal User caller
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createPricing(caller, request));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<CenterServicePricingResponse> updatePricing(
            @PathVariable Long id,
            @RequestBody @Valid UpdatePricingRequest request,
            @AuthenticationPrincipal User caller
    ) {
        return ResponseEntity.ok(service.updatePricing(caller, id, request));
    }
    
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePricing(
            @PathVariable Long id,
            @AuthenticationPrincipal User caller
    ) {
        service.deletePricing(caller, id);
    }
}