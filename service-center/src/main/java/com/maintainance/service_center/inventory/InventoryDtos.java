package com.maintainance.service_center.inventory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Spec 025 — inventory request/response records (package-private). */
final class InventoryDtos {
    private InventoryDtos() {}
}

// ── Requests ──
record CreatePartRequest(
        @NotBlank String nameAr, @NotBlank String nameEn, @NotBlank String sku,
        String category, @NotNull Unit unit,
        @NotNull @DecimalMin("0.0") BigDecimal costPrice,
        @NotNull @DecimalMin("0.0") BigDecimal salePrice,
        String supplier, @Min(0) int reorderThreshold) {}

record UpdatePartRequest(
        @NotBlank String nameAr, @NotBlank String nameEn, @NotBlank String sku,
        String category, @NotNull Unit unit,
        @NotNull @DecimalMin("0.0") BigDecimal costPrice,
        @NotNull @DecimalMin("0.0") BigDecimal salePrice,
        String supplier, @Min(0) int reorderThreshold, Boolean isActive) {}

record ReceiveStockRequest(@Min(1) int quantity, BigDecimal unitCost) {}

record AdjustStockRequest(@NotNull Integer newOnHand, @NotBlank String reason) {}

// ── Responses ──
record PartResponse(
        Long id, String nameAr, String nameEn, String sku, String category, Unit unit,
        BigDecimal costPrice, BigDecimal salePrice, String supplier,
        int reorderThreshold, int onHand, boolean isActive) {}

record MovementResponse(
        Long id, Long partId, MovementType type, int quantity, BigDecimal unitCost,
        String reason, Long bookingId, String actorName, LocalDateTime createdAt) {}

record LowStockItem(
        Long partId, String nameAr, String nameEn, String sku, int onHand,
        int reorderThreshold, String supplier, int suggestedReorderQty) {}

record UsageRow(Long partId, String nameEn, String nameAr, int consumedQty) {}

record InventoryReportResponse(
        LocalDate from, LocalDate to, BigDecimal stockValue, List<UsageRow> usage,
        List<Long> fastMovers, List<Long> slowMovers, BigDecimal partsMargin,
        List<LowStockItem> reorder) {}
