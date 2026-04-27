package com.maintainance.service_center.booking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Page<Booking> findByCustomer_IdOrderByCreatedAtDesc(Integer customerId, Pageable pageable);

    Page<Booking> findByCenter_IdOrderByCreatedAtDesc(Long centerId, Pageable pageable);

    Optional<Booking> findByBookingNumber(String bookingNumber);

    @Query("SELECT b FROM Booking b WHERE b.customer.id = :customerId AND b.bookingStatus IN :statuses")
    List<Booking> findByCustomerIdAndStatuses(@Param("customerId") Integer customerId, @Param("statuses") List<BookingStatus> statuses);

    @Query("SELECT b FROM Booking b WHERE b.center.id = :centerId AND b.bookingStatus IN :statuses")
    List<Booking> findByCenterIdAndStatuses(@Param("centerId") Long centerId, @Param("statuses") List<BookingStatus> statuses);

    @Query("SELECT b FROM Booking b WHERE b.center.id = :centerId AND b.bookingStatus IN :statuses ORDER BY b.createdAt DESC")
    Page<Booking> findByCenterIdAndStatusesPageable(@Param("centerId") Long centerId, @Param("statuses") List<BookingStatus> statuses, Pageable pageable);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.center.id = :centerId AND b.bookingStatus = :status")
    long countByCenterIdAndStatus(@Param("centerId") Long centerId, @Param("status") BookingStatus status);

    long countByCenterId(Long centerId);

    boolean existsByBookingNumber(String bookingNumber);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.center.owner.id = :ownerId AND b.bookingStatus = :status")
    Long countByOwnerIdAndStatus(@Param("ownerId") Integer ownerId, @Param("status") BookingStatus status);

    @Query("SELECT SUM(b.finalCost) FROM Booking b WHERE b.center.owner.id = :ownerId AND b.bookingStatus = 'COMPLETED'")
    BigDecimal sumFinalCostByOwnerId(@Param("ownerId") Integer ownerId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.center.owner.id = :ownerId")
    Long countByOwnerId(@Param("ownerId") Integer ownerId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.customer.id = :customerId")
    Long countByCustomerId(@Param("customerId") Integer customerId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.customer.id = :customerId AND b.bookingStatus = :status")
    Long countByCustomerIdAndStatus(@Param("customerId") Integer customerId, @Param("status") BookingStatus status);

    @Query("SELECT SUM(b.finalCost) FROM Booking b WHERE b.customer.id = :customerId AND b.bookingStatus = 'COMPLETED'")
    BigDecimal sumFinalCostByCustomerId(@Param("customerId") Integer customerId);

    // Analytics methods
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.center.id = :centerId AND b.createdAt BETWEEN :startDate AND :endDate")
    long countByCenterIdAndDateRange(@Param("centerId") Long centerId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.center.id = :centerId AND b.bookingStatus = :status AND b.createdAt BETWEEN :startDate AND :endDate")
    long countByCenterIdAndStatusAndDateRange(@Param("centerId") Long centerId, @Param("status") BookingStatus status, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(b.finalCost), 0) FROM Booking b WHERE b.center.id = :centerId AND b.bookingStatus = :status AND b.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumFinalCostByCenterAndDateRange(@Param("centerId") Long centerId, @Param("status") BookingStatus status, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT b.serviceType, COUNT(b), COALESCE(SUM(b.finalCost), 0) FROM Booking b WHERE b.center.id = :centerId AND b.bookingStatus = :status AND b.createdAt BETWEEN :startDate AND :endDate GROUP BY b.serviceType")
    List<Object[]> countCompletedBookingsByServiceTypeAndDateRange(@Param("centerId") Long centerId, @Param("status") BookingStatus status, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT EXTRACT(HOUR FROM created_at)::integer AS hour, COUNT(*) AS count " +
                   "FROM booking WHERE center_id = :centerId AND created_at BETWEEN :startDate AND :endDate " +
                   "GROUP BY EXTRACT(HOUR FROM created_at)::integer ORDER BY hour",
           nativeQuery = true)
    List<Object[]> countBookingsByHourAndDateRange(@Param("centerId") Long centerId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
