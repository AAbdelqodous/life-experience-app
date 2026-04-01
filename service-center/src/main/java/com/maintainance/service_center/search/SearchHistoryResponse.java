package com.maintainance.service_center.search;

import com.maintainance.service_center.category.ServiceCategory;
import com.maintainance.service_center.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchHistoryResponse {
    private Long id;
    private Long userId;
    private String userFirstname;
    private String userLastname;
    private String searchQuery;
    private Long categoryId;
    private String categoryNameAr;
    private String categoryNameEn;
    private String source;
    private Integer resultsCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;

    public static SearchHistoryResponse fromEntity(SearchHistory searchHistory) {
        User user = searchHistory.getUser();
        ServiceCategory category = searchHistory.getCategory();
        
        return SearchHistoryResponse.builder()
                .id(searchHistory.getId())
                .userId(user != null ? user.getId().longValue() : null)
                .userFirstname(user != null ? user.getFirstname() : null)
                .userLastname(user != null ? user.getLastname() : null)
                .searchQuery(searchHistory.getSearchQuery())
                .categoryId(category != null ? category.getId() : null)
                .categoryNameAr(category != null ? category.getNameAr() : null)
                .categoryNameEn(category != null ? category.getNameEn() : null)
                .source(searchHistory.getSource() != null ? searchHistory.getSource().name() : null)
                .resultsCount(searchHistory.getResultsCount())
                .createdAt(searchHistory.getCreatedAt())
                .lastUsedAt(searchHistory.getLastUsedAt())
                .build();
    }
}
