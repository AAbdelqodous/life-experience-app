package com.maintainance.service_center.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByCenterIdOrderByCreatedAtDesc(Long centerId, Pageable pageable);

    Page<Review> findByReviewerIdOrderByCreatedAtDesc(Integer reviewerId, Pageable pageable);

    Optional<Review> findByBookingId(Long bookingId);

    @Query("SELECT AVG (r.rating) FROM Review r WHERE r.center.id = : centerId")
    Double calculateAverageRating(@Param("centerId") Long centerId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.center.id = :centerId")
    Integer countByCenterId(@Param("centerId") Long centerId);

    boolean existsByBookingIdAndReviewerId(Long bookingId, Integer reviewerId);
}
