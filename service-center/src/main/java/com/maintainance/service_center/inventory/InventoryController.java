package com.maintainance.service_center.inventory;

import com.maintainance.service_center.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Spec 025 — center inventory: parts catalog, stock movements, low-stock, and reports. All endpoints
 * are center-scoped (active center) and gated per the contract. Consumption is not exposed here — it
 * happens server-side when a quote with part lines is committed (and reverses on cancel).
 */
@RestController
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService service;

    @GetMapping("/centers/my/parts")
    public ResponseEntity<List<PartResponse>> listParts(
            @AuthenticationPrincipal User caller,
            @RequestParam(required = false) String search,
            @RequestParam(name = "lowStock", defaultValue = "false") boolean lowStock) {
        return ResponseEntity.ok(service.listParts(caller, search, lowStock));
    }

    @PostMapping("/centers/my/parts")
    public ResponseEntity<PartResponse> createPart(
            @AuthenticationPrincipal User caller, @Valid @RequestBody CreatePartRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createPart(caller, request));
    }

    @PutMapping("/centers/my/parts/{id}")
    public ResponseEntity<PartResponse> updatePart(
            @AuthenticationPrincipal User caller, @PathVariable Long id,
            @Valid @RequestBody UpdatePartRequest request) {
        return ResponseEntity.ok(service.updatePart(caller, id, request));
    }

    @DeleteMapping("/centers/my/parts/{id}")
    public ResponseEntity<Void> deactivatePart(@AuthenticationPrincipal User caller, @PathVariable Long id) {
        service.deactivatePart(caller, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/centers/my/parts/{id}/movements")
    public ResponseEntity<List<MovementResponse>> movements(
            @AuthenticationPrincipal User caller, @PathVariable Long id) {
        return ResponseEntity.ok(service.getMovements(caller, id));
    }

    @PostMapping("/centers/my/parts/{id}/receive")
    public ResponseEntity<PartResponse> receive(
            @AuthenticationPrincipal User caller, @PathVariable Long id,
            @Valid @RequestBody ReceiveStockRequest request) {
        return ResponseEntity.ok(service.receiveStock(caller, id, request));
    }

    @PostMapping("/centers/my/parts/{id}/adjust")
    public ResponseEntity<PartResponse> adjust(
            @AuthenticationPrincipal User caller, @PathVariable Long id,
            @Valid @RequestBody AdjustStockRequest request) {
        return ResponseEntity.ok(service.adjustStock(caller, id, request));
    }

    @GetMapping("/centers/my/inventory/low-stock")
    public ResponseEntity<List<LowStockItem>> lowStock(@AuthenticationPrincipal User caller) {
        return ResponseEntity.ok(service.getLowStock(caller));
    }

    @GetMapping("/centers/my/inventory/report")
    public ResponseEntity<InventoryReportResponse> report(
            @AuthenticationPrincipal User caller,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(service.getReport(caller, from, to));
    }
}
