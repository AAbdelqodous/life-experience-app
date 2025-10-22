package com.maintainance.service_center.booking;

import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Find by booking number
    Optional<Booking> findByBookingNumber(String bookingNumber);

    // Find bookings by customer
    List<Booking> findByCustomer(User customer);

    List<Booking> findByCustomerId(Long customerId);

    List<Booking> findByCustomerOrderByCreatedAtDesc(User customer);

    // Find bookings by center
    List<Booking> findByCenter(MaintenanceCenter center);

    List<Booking> findByCenterId(Long centerId);

    List<Booking> findByCenterOrderByBookingDateAscBookingTimeAsc(MaintenanceCenter center);

    // Find by booking status
    List<Booking> findByBookingStatus(BookingStatus status);

    List<Booking> findByBookingStatusOrderByBookingDateAscBookingTimeAsc(BookingStatus status);

    // Find by customer and status
    List<Booking> findByCustomerAndBookingStatus(User customer, BookingStatus status);

    List<Booking> findByCustomerIdAndBookingStatus(Long customerId, BookingStatus status);

    // Find by center and status
    List<Booking> findByCenterAndBookingStatus(MaintenanceCenter center, BookingStatus status);

    List<Booking> findByCenterIdAndBookingStatus(Long centerId, BookingStatus status);

    // Find by date
    List<Booking> findByBookingDate(LocalDate date);

    List<Booking> findByBookingDateBetween(LocalDate startDate, LocalDate endDate);

    List<Booking> findByBookingDateAfter(LocalDate date);

    List<Booking> findByBookingDateBefore(LocalDate date);

    // Find by center and date
    List<Booking> findByCenterAndBookingDate(MaintenanceCenter center, LocalDate date);

    List<Booking> findByCenterIdAndBookingDate(Long centerId, LocalDate date);

    List<Booking> findByCenterAndBookingDateBetween(MaintenanceCenter center, LocalDate startDate, LocalDate endDate);

    // Find by center, date and status
    List<Booking> findByCenterAndBookingDateAndBookingStatus(MaintenanceCenter center, LocalDate date, BookingStatus status);

    List<Booking> findByCenterIdAndBookingDateAndBookingStatus(Long centerId, LocalDate date, BookingStatus status);

    // Find by payment status
    List<Booking> findByPaymentStatus(PaymentStatus paymentStatus);

    List<Booking> findByBookingStatusAndPaymentStatus(BookingStatus bookingStatus, PaymentStatus paymentStatus);

    // Find urgent bookings
    List<Booking> findByIsUrgentTrue();

    List<Booking> findByCenterAndIsUrgentTrue(MaintenanceCenter center);

    List<Booking> findByCenterIdAndIsUrgentTrue(Long centerId);

    // Find bookings needing reminders
    @Query("SELECT b FROM Booking b WHERE b.bookingDate = :date AND (b.reminderSent = false OR b.reminderSent IS NULL) AND b.bookingStatus = 'CONFIRMED'")
    List<Booking> findBookingsNeedingReminder(@Param("date") LocalDate date);

    // Count queries
    long countByCenter(MaintenanceCenter center);

    long countByCenterId(Long centerId);

    long countByBookingStatus(BookingStatus status);

    long countByCenterAndBookingStatus(MaintenanceCenter center, BookingStatus status);

    long countByCenterIdAndBookingStatus(Long centerId, BookingStatus status);

    long countByCustomer(User customer);

    long countByCustomerId(Long customerId);

    // Check existence
    boolean existsByBookingNumber(String bookingNumber);

    boolean existsByCenterAndBookingDateAndBookingTime(MaintenanceCenter center, LocalDate date, java.time.LocalTime time);

    // Find by service type
    List<Booking> findByServiceType(ServiceType serviceType);

    List<Booking> findByCenterAndServiceType(MaintenanceCenter center, ServiceType serviceType);

    // Find completed bookings without review
    @Query("SELECT b FROM Booking b WHERE b.bookingStatus = 'COMPLETED' AND b.review IS NULL")
    List<Booking> findCompletedBookingsWithoutReview();

    @Query("SELECT b FROM Booking b WHERE b.customer.id = :customerId AND b.bookingStatus = 'COMPLETED' AND b.review IS NULL")
    List<Booking> findCompletedBookingsWithoutReviewByCustomer(@Param("customerId") Long customerId);

    // Custom query for dashboard statistics
    @Query("SELECT b.bookingStatus, COUNT(b) FROM Booking b WHERE b.center.id = :centerId GROUP BY b.bookingStatus")
    List<Object[]> getBookingStatusCountsByCenter(@Param("centerId") Long centerId);

    @Query("SELECT b FROM Booking b WHERE b.center.id = :centerId AND b.bookingDate BETWEEN :startDate AND :endDate ORDER BY b.bookingDate ASC, b.bookingTime ASC")
    List<Booking> findByCenterAndDateRange(@Param("centerId") Long centerId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
