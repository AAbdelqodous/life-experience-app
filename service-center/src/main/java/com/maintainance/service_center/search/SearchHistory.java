package com.maintainance.service_center.search;

import com.maintainance.service_center.category.ServiceCategory;
import com.maintainance.service_center.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "search_history")
@EntityListeners(AuditingEntityListener.class)
public class SearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String searchQuery;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ServiceCategory category;

    // Search location
    private Double latitude;
    private Double longitude;
    private String city;

    // Search filters applied
    private Double minRating;
    private Double maxDistance; // in kilometers
    private String priceRange;
    private Boolean verifiedOnly;

    // Results
    private Integer resultsCount;
    private Long selectedCenterId; // Which center the user clicked on

    @Enumerated(EnumType.STRING)
    private SearchSource source;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastUsedAt;
}
