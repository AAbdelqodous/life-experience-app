package com.maintainance.service_center.search;

import com.maintainance.service_center.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
    
    List<SearchHistory> findByUserOrderByCreatedAtDesc(User user);
    
    Page<SearchHistory> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    List<SearchHistory> findByUserAndSearchQueryContainingIgnoreCaseOrderByCreatedAtDesc(User user, String query);
    
    List<SearchHistory> findTop10ByUserOrderByCreatedAtDesc(User user);
    
    List<SearchHistory> findByUserAndCategory_IdOrderByCreatedAtDesc(User user, Integer categoryId);
    
    List<SearchHistory> findByUserAndSourceOrderByCreatedAtDesc(User user, SearchSource source);
    
    List<SearchHistory> findByUserAndCreatedAtBefore(User user, LocalDateTime before);
}
