package com.maintainance.service_center.reroute;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Append-only repository for {@link RerouteAudit} (spec 022 FR-DR-024).
 * <p>Extends the bare {@link Repository} marker so callers can only insert and read —
 * no {@code delete} or bulk-modify methods are exposed.
 */
@org.springframework.stereotype.Repository
public interface RerouteAuditRepository extends Repository<RerouteAudit, Long> {

    RerouteAudit save(RerouteAudit audit);

    Optional<RerouteAudit> findById(Long id);

    List<RerouteAudit> findByBookingIdOrderByCreatedAtAsc(Long bookingId);

    long countByBookingId(Long bookingId);
}
