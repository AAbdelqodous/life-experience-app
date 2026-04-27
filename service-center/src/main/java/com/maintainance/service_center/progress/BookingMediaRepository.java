package com.maintainance.service_center.progress;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingMediaRepository extends JpaRepository<BookingMedia, Long> {

    /**
     * Find all media for a booking that is visible to customers
     * @param bookingId the booking ID
     * @return list of booking media visible to customers
     */
    List<BookingMedia> findByBookingIdAndIsVisibleToCustomerTrue(Long bookingId);
    
    /**
     * Find all media for a booking
     * @param bookingId the booking ID
     * @return list of all booking media for the booking
     */
    List<BookingMedia> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
}
