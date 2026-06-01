package com.maintainance.service_center.quoterequest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface QuoteRequestRepository extends JpaRepository<QuoteRequest, Long> {

    /** A customer's own requests, newest first. */
    List<QuoteRequest> findByCustomerIdOrderByCreatedAtDesc(Integer customerId);

    /**
     * Center inbox: OPEN requests whose category is one this center covers.
     * The caller resolves the center's covered category ids (matching is server-side, spec 024 R2).
     */
    @Query("""
            select qr from QuoteRequest qr
            where qr.status = com.maintainance.service_center.quoterequest.QuoteRequestStatus.OPEN
              and qr.category.id in :categoryIds
            order by qr.createdAt desc
            """)
    List<QuoteRequest> findOpenForCategories(@Param("categoryIds") List<Long> categoryIds);

    /** Expiry sweep: OPEN requests past their window. */
    List<QuoteRequest> findByStatusAndExpiresAtBefore(QuoteRequestStatus status, LocalDateTime cutoff);
}
