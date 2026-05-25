package com.maintainance.service_center.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingClaimAuditRepository extends JpaRepository<BookingClaimAudit, Long> {
}
