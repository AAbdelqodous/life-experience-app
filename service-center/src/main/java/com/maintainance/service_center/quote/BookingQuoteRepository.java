package com.maintainance.service_center.quote;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingQuoteRepository extends JpaRepository<BookingQuote, Long> {

    List<BookingQuote> findByBookingIdOrderByVersionDesc(Long bookingId);

    Optional<BookingQuote> findByIdAndBookingId(Long id, Long bookingId);

    int countByBookingId(Long bookingId);
}
