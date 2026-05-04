package com.maintainance.service_center.lookup;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/lookups")
@RequiredArgsConstructor
@Tag(name = "Lookups", description = "General-purpose lookup master-detail API")
public class LookupController {

    private final LookupService lookupService;

    // ── Public GET endpoints (no auth required) ───────────────────────────────────

    @GetMapping
    @Operation(summary = "List all active lookups", description = "Returns all active lookup masters without details")
    public ResponseEntity<List<LookupResponse>> getAllActive() {
        return ResponseEntity.ok(lookupService.getAllActive());
    }

    @GetMapping("/bulk")
    @Operation(summary = "Bulk fetch details", description = "Returns active details for multiple lookup codes in one call. Pass codes as comma-separated query param.")
    public ResponseEntity<Map<String, List<LookupDetailResponse>>> getBulk(
            @RequestParam List<String> codes) {
        return ResponseEntity.ok(lookupService.getBulk(codes));
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get lookup with details", description = "Returns the lookup master and all active detail values. Supports ETag for conditional requests.")
    public ResponseEntity<LookupResponse> getByCode(
            @PathVariable String code,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        LookupResponse response = lookupService.getByCode(code);
        String etag = "\"" + response.getVersion() + "\"";
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }
        return ResponseEntity.ok().eTag(etag).body(response);
    }

    @GetMapping("/{code}/details")
    @Operation(summary = "Get active details", description = "Returns only the active detail values for a lookup code, sorted by sort_order")
    public ResponseEntity<List<LookupDetailResponse>> getActiveDetails(@PathVariable String code) {
        return ResponseEntity.ok(lookupService.getActiveDetails(code));
    }

    @GetMapping("/{code}/details/{detailCode}")
    @Operation(summary = "Get single detail value", description = "Returns a single active detail value by its code")
    public ResponseEntity<LookupDetailResponse> getDetail(
            @PathVariable String code,
            @PathVariable String detailCode) {
        return ResponseEntity.ok(lookupService.getDetail(code, detailCode));
    }

    // ── Admin GET endpoints (ROLE_ADMIN, path-gated by SecurityConfig) ───────────

    @GetMapping("/admin/all")
    @Operation(summary = "[Admin] List all lookups", description = "Returns all lookup masters including inactive ones")
    public ResponseEntity<List<LookupResponse>> getAll() {
        return ResponseEntity.ok(lookupService.getAll());
    }

    @GetMapping("/admin/{code}/details")
    @Operation(summary = "[Admin] List all details", description = "Returns all details for a lookup including inactive ones")
    public ResponseEntity<List<LookupDetailResponse>> getAllDetails(@PathVariable String code) {
        return ResponseEntity.ok(lookupService.getAllDetails(code));
    }

    // ── Admin write endpoints (ROLE_ADMIN, method-gated by SecurityConfig) ────────

    @PostMapping
    @Operation(summary = "[Admin] Create lookup", description = "Creates a new lookup master category")
    public ResponseEntity<LookupResponse> createLookup(@RequestBody @Valid LookupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lookupService.createLookup(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "[Admin] Update lookup", description = "Updates an existing lookup master by numeric ID")
    public ResponseEntity<LookupResponse> updateLookup(
            @PathVariable Long id,
            @RequestBody @Valid LookupRequest request) {
        return ResponseEntity.ok(lookupService.updateLookup(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "[Admin] Delete lookup", description = "Soft-deletes a lookup master (is_active = false). Blocked if is_system = true.")
    public ResponseEntity<Void> deleteLookup(@PathVariable Long id) {
        lookupService.deleteLookup(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{code}/details")
    @Operation(summary = "[Admin] Add detail value", description = "Adds a new detail value to an existing lookup")
    public ResponseEntity<LookupDetailResponse> addDetail(
            @PathVariable String code,
            @RequestBody @Valid LookupDetailRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lookupService.addDetail(code, request));
    }

    @PutMapping("/details/{id}")
    @Operation(summary = "[Admin] Update detail value", description = "Updates an existing detail value by numeric ID")
    public ResponseEntity<LookupDetailResponse> updateDetail(
            @PathVariable Long id,
            @RequestBody @Valid LookupDetailRequest request) {
        return ResponseEntity.ok(lookupService.updateDetail(id, request));
    }

    @DeleteMapping("/details/{id}")
    @Operation(summary = "[Admin] Delete detail value", description = "Soft-deletes a detail value (is_active = false). Blocked if is_system = true.")
    public ResponseEntity<Void> deleteDetail(@PathVariable Long id) {
        lookupService.deleteDetail(id);
        return ResponseEntity.noContent().build();
    }
}
