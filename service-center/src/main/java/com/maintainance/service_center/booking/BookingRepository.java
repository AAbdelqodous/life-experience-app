package com.maintainance.service_center.booking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Page<Booking> findByCustomerIdOrderByCreatedAtDesc(Integer customerId, Pageable pageable);

    Page<Booking> findByCenterIdOrderByCreatedAtDesc(Long centerId, Pageable pageable);

    Optional<Booking> findByBookingNumber(String bookingNumber);

    @Query("SELECT b FROM Booking b WHERE b.customer.id = :customerId AND b.bookingStatus IN :statuses")
    List<Booking> findByCustomerIdAndStatuses(@Param("customerId") Integer customerId, @Param("statuses") List<BookingStatus> statuses);

    @Query("SELECT b FROM Booking b WHERE b.center.id = :centerId AND b.bookingStatus IN :statuses")
    List<Booking> findByCenterIdAndStatuses(@Param("centerId") Long centerId, @Param("statuses") List<BookingStatus> statuses);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.center.id = :centerId AND b.bookingStatus = :status")
    long countByCenterIdAndStatus(@Param("centerId") Long centerId, @Param("status") BookingStatus status);

    long countByCenterId(Long centerId);

    boolean existsByBookingNumber(String bookingNumber);
}
