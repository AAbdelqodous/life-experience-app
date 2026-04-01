package com.maintainance.service_center.search;

import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.center.MaintenanceCenterSummaryResponse;
import com.maintainance.service_center.category.ServiceCategory;
import com.maintainance.service_center.category.ServiceCategoryRepository;
import com.maintainance.service_center.user.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final SearchHistoryRepository searchHistoryRepository;
    private final MaintenanceCenterRepository maintenanceCenterRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;

    /**
     * Search centers with filters
     */
    public Page<MaintenanceCenterSummaryResponse> searchCenters(SearchRequest request, Pageable pageable) {
        log.info("Searching centers with filters: {}", request);

        // Validate latitude and longitude if provided
        if (request.getLatitude() != null && (request.getLatitude() < -90 || request.getLatitude() > 90)) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (request.getLongitude() != null && (request.getLongitude() < -180 || request.getLongitude() > 180)) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }

        Page<MaintenanceCenterSummaryResponse> results;

        // Determine if location-based search is needed
        boolean hasLocation = request.getLatitude() != null && request.getLongitude() != null;
        boolean hasMaxDistance = request.getMaxDistance() != null;

        if (hasLocation && hasMaxDistance) {
            // Use location-based search with all filters
            results = maintenanceCenterRepository.searchCentersWithLocation(
                    request.getQuery(),
                    request.getCategoryId(),
                    request.getMinRating(),
                    request.getVerifiedOnly(),
                    request.getLatitude(),
                    request.getLongitude(),
                    request.getMaxDistance(),
                    pageable
            ).map(this::mapToSummaryResponse);
        } else if (hasLocation) {
            // Use basic search with location filter (no max distance)
            results = maintenanceCenterRepository.findByLocation(
                    request.getLatitude(),
                    request.getLongitude(),
                    null,
                    pageable
            ).map(this::mapToSummaryResponse);
        } else {
            // Use basic search without location
            results = maintenanceCenterRepository.searchCenters(
                    request.getQuery(),
                    request.getCategoryId(),
                    request.getMinRating(),
                    request.getVerifiedOnly(),
                    pageable
            ).map(this::mapToSummaryResponse);
        }

        return results;
    }

    /**
     * Get search history for current user
     */
    public Page<SearchHistoryResponse> getSearchHistory(Pageable pageable, User user) {
        Page<SearchHistory> history = searchHistoryRepository
                .findByUserOrderByCreatedAtDesc(user, pageable);
        return history.map(this::mapToResponse);
    }

    /**
     * Get recent searches (top 10)
     */
    public List<SearchHistoryResponse> getRecentSearches(User user) {
        List<SearchHistory> history = searchHistoryRepository
                .findTop10ByUserOrderByCreatedAtDesc(user);
        return history.stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Save search history
     */
    @Transactional
    public void saveSearchHistory(SearchRequest request, Integer resultsCount, User user) {
        
        SearchHistory history = SearchHistory.builder()
                .user(user)
                .searchQuery(request.getQuery())
                .category(request.getCategoryId() != null ? 
                        serviceCategoryRepository.findById(request.getCategoryId()).orElse(null) : null)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .city(request.getCity())
                .minRating(request.getMinRating())
                .maxDistance(request.getMaxDistance())
                .priceRange(request.getPriceRange())
                .verifiedOnly(request.getVerifiedOnly())
                .resultsCount(resultsCount != null ? resultsCount : 0)
                .source(SearchSource.MANUAL)
                .build();
        
        searchHistoryRepository.save(history);
        log.info("Search history saved for user ID: {}", user.getId());
    }

    /**
     * Delete search history by ID
     */
    @Transactional
    public void deleteSearchHistory(Long id, User user) {
        SearchHistory history = searchHistoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Search history not found"));
        
        if (!history.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You can only delete your own search history");
        }
        
        searchHistoryRepository.delete(history);
        log.info("Search history ID {} deleted for user ID: {}", id, user.getId());
    }

    /**
     * Clear all search history for current user
     */
    @Transactional
    public void clearSearchHistory(User user) {
        List<SearchHistory> history = searchHistoryRepository.findByUserOrderByCreatedAtDesc(user);
        searchHistoryRepository.deleteAll(history);
        log.info("All search history cleared for user ID: {}", user.getId());
    }

    /**
     * Get popular searches (based on frequency)
     * Returns searches that have been performed multiple times across all users
     */
    public List<SearchHistoryResponse> getPopularSearches(User user) {
        // Get user's recent searches as a fallback
        // For a proper implementation, you would need to add a custom query to SearchHistoryRepository
        // that aggregates searches by query and returns the most frequent ones
        return getRecentSearches(user);
    }

    /**
     * Map MaintenanceCenter entity to MaintenanceCenterSummaryResponse
     */
    private MaintenanceCenterSummaryResponse mapToSummaryResponse(MaintenanceCenter center) {
        Long categoryId = (center.getCategories() != null && !center.getCategories().isEmpty())
                ? center.getCategories().get(0).getId()
                : null;
        return MaintenanceCenterSummaryResponse.builder()
                .id(center.getId())
                .nameAr(center.getNameAr())
                .nameEn(center.getNameEn())
                .descriptionAr(center.getDescriptionAr())
                .descriptionEn(center.getDescriptionEn())
                .averageRating(center.getAverageRating())
                .totalReviews(center.getTotalReviews())
                .phone(center.getPhone())
                .address(center.getAddress())
                .latitude(center.getLatitude())
                .longitude(center.getLongitude())
                .isActive(center.getIsActive())
                .isVerified(center.getIsVerified())
                .categoryId(categoryId)
                .build();
    }

    /**
     * Map SearchHistory entity to SearchHistoryResponse
     */
    private SearchHistoryResponse mapToResponse(SearchHistory history) {
        return SearchHistoryResponse.builder()
                .id(history.getId())
                .userId(history.getUser() != null ? history.getUser().getId().longValue() : null)
                .userFirstname(history.getUser() != null ? history.getUser().getFirstname() : null)
                .userLastname(history.getUser() != null ? history.getUser().getLastname() : null)
                .searchQuery(history.getSearchQuery())
                .categoryId(history.getCategory() != null ? 
                        history.getCategory().getId() : null)
                .categoryNameAr(history.getCategory() != null ? 
                        history.getCategory().getNameAr() : null)
                .categoryNameEn(history.getCategory() != null ? 
                        history.getCategory().getNameEn() : null)
                .source(history.getSource() != null ? history.getSource().name() : null)
                .resultsCount(history.getResultsCount())
                .createdAt(history.getCreatedAt())
                .lastUsedAt(history.getLastUsedAt())
                .build();
    }
}
