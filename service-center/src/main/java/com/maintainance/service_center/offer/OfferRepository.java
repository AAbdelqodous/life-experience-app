package com.maintainance.service_center.offer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OfferRepository extends JpaRepository<CenterOffer, Long> {

    List<CenterOffer> findByCenterIdOrderByCreatedAtDesc(Long centerId);

    Optional<CenterOffer> findByIdAndCenterId(Long id, Long centerId);

    @Query("SELECT COUNT(o) FROM CenterOffer o WHERE o.center.id = :centerId " +
           "AND o.cancelledAt IS NULL AND o.endDate >= :today")
    long countActiveOrScheduled(@Param("centerId") Long centerId,
                                @Param("today") LocalDate today);
}
