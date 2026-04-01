package com.maintainance.service_center.search;

import com.maintainance.service_center.center.MaintenanceCenterSummaryResponse;
import com.maintainance.service_center.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Search and filter endpoints")
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/centers")
    @Operation(summary = "Search centers", description = "Search maintenance centers with filters")
    public ResponseEntity<Page<MaintenanceCenterSummaryResponse>> searchCenters(
            @Valid SearchRequest request,
            Pageable pageable) {
        Page<MaintenanceCenterSummaryResponse> response = searchService.searchCenters(request, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    @Operation(summary = "Get search history", description = "Retrieve paginated search history for current user")
    public ResponseEntity<Page<SearchHistoryResponse>> getSearchHistory(
            Pageable pageable,
            @AuthenticationPrincipal User caller) {
        Page<SearchHistoryResponse> response = searchService.getSearchHistory(pageable, caller);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/recent")
    @Operation(summary = "Get recent searches", description = "Retrieve top 10 recent searches")
    public ResponseEntity<java.util.List<SearchHistoryResponse>> getRecentSearches(
            @AuthenticationPrincipal User caller) {
        java.util.List<SearchHistoryResponse> response = searchService.getRecentSearches(caller);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/popular")
    @Operation(summary = "Get popular searches", description = "Retrieve popular searches based on frequency")
    public ResponseEntity<java.util.List<SearchHistoryResponse>> getPopularSearches(
            @AuthenticationPrincipal User caller) {
        java.util.List<SearchHistoryResponse> response = searchService.getPopularSearches(caller);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Save search", description = "Save a search query to history")
    public ResponseEntity<Void> saveSearch(
            @Valid @RequestBody SearchRequest request,
            @AuthenticationPrincipal User caller) {
        searchService.saveSearchHistory(request, 0, caller);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/with-results")
    @Operation(summary = "Save search with results", description = "Save a search query with results count")
    public ResponseEntity<Void> saveSearchWithResults(
            @Valid @RequestBody SearchRequest request,
            @RequestParam Integer resultsCount,
            @AuthenticationPrincipal User caller) {
        searchService.saveSearchHistory(request, resultsCount, caller);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/history/{id}")
    @Operation(summary = "Delete search history", description = "Delete a search history entry by ID")
    public ResponseEntity<Void> deleteSearchHistory(
            @PathVariable Long id,
            @AuthenticationPrincipal User caller) {
        searchService.deleteSearchHistory(id, caller);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/history")
    @Operation(summary = "Clear search history", description = "Clear all search history for current user")
    public ResponseEntity<Void> clearSearchHistory(
            @AuthenticationPrincipal User caller) {
        searchService.clearSearchHistory(caller);
        return ResponseEntity.ok().build();
    }
}
