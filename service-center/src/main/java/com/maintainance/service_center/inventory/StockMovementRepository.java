package com.maintainance.service_center.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByPartIdOrderByCreatedAtDesc(Long partId);

    /** All movements for a booking — used to compute the outstanding consumption to reverse (R5). */
    List<StockMovement> findByBookingId(Long bookingId);

    /** All movements in a window for one center — drives usage/margin in the inventory report. */
    List<StockMovement> findByPart_Center_IdAndCreatedAtBetween(
            Long centerId, LocalDateTime from, LocalDateTime to);
}
