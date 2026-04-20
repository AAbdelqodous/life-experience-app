package com.maintainance.service_center.progress;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingWorkProgressRepository extends JpaRepository<BookingWorkProgress, Long> {

    /**
     * Find all progress entries for a booking, ordered by creation date ascending
     * @param bookingId the booking ID
     * @return list of booking work progress entries ordered by createdAt ASC
     */
    List<BookingWorkProgress> findByBookingIdOrderByCreatedAtAsc(Long bookingId);
}
