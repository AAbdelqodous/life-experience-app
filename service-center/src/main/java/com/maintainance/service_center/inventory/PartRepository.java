package com.maintainance.service_center.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PartRepository extends JpaRepository<Part, Long> {

    List<Part> findByCenterIdOrderByNameEnAsc(Long centerId);

    Optional<Part> findByIdAndCenterId(Long id, Long centerId);

    boolean existsByCenterIdAndSku(Long centerId, String sku);

    @Query("""
            SELECT p FROM Part p WHERE p.center.id = :centerId AND (
                LOWER(p.nameEn) LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(p.nameAr) LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(p.sku)    LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY p.nameEn ASC
            """)
    List<Part> search(@Param("centerId") Long centerId, @Param("q") String q);

    @Query("SELECT p FROM Part p WHERE p.center.id = :centerId AND p.isActive = true " +
           "AND p.onHand <= p.reorderThreshold ORDER BY p.onHand ASC")
    List<Part> findLowStock(@Param("centerId") Long centerId);

    /** Atomic stock change — no read-modify-write, so concurrent consumers can't double-spend (R3). */
    @Modifying
    @Query("UPDATE Part p SET p.onHand = p.onHand - :qty WHERE p.id = :id")
    void decrementOnHand(@Param("id") Long id, @Param("qty") int qty);

    @Modifying
    @Query("UPDATE Part p SET p.onHand = p.onHand + :qty WHERE p.id = :id")
    void incrementOnHand(@Param("id") Long id, @Param("qty") int qty);
}
