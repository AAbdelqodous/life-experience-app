package com.maintainance.service_center.booking;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Write-once audit log for self-claim events (spec 021 FR-SC-011, FR-SC-012).
 * <p>Extends the bare {@link Repository} marker rather than {@code JpaRepository} so callers
 * can only insert and read — no {@code delete}, no bulk-modify methods are exposed.
 */
@org.springframework.stereotype.Repository
public interface BookingClaimAuditRepository extends Repository<BookingClaimAudit, Long> {

    BookingClaimAudit save(BookingClaimAudit audit);

    Optional<BookingClaimAudit> findById(Long id);

    List<BookingClaimAudit> findByBookingId(Long bookingId);

    List<BookingClaimAudit> findByMembershipId(Long membershipId);

    long count();
}
