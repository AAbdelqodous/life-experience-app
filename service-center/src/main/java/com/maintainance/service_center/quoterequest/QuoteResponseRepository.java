package com.maintainance.service_center.quoterequest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuoteResponseRepository extends JpaRepository<QuoteResponse, Long> {

    /** All responses on a request (customer view). */
    List<QuoteResponse> findByRequestId(Long requestId);

    /** This center's single response on a request (sealed center view). */
    Optional<QuoteResponse> findByRequestIdAndCenterId(Long requestId, Long centerId);
}
