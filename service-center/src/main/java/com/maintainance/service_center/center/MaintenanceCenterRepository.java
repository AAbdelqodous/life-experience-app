package com.maintainance.service_center.center;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MaintenanceCenterRepository extends JpaRepository<MaintenanceCenter, Long> {

    Page<MaintenanceCenter> findByIsActiveTrue(Pageable pageable);

    Page<MaintenanceCenter> findByOwnerIdAndIsActiveTrue(Integer ownerId, Pageable pageable);

    @Query("SELECT c FROM MaintenanceCenter c WHERE c.isActive = true AND " +
           "(LOWER(c.nameAr) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.nameEn) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<MaintenanceCenter> searchByName(@Param("query") String query, Pageable pageable);

    @Query("SELECT c FROM MaintenanceCenter c JOIN c.categories cat WHERE cat.id = :categoryId AND c.isActive = true")
    Page<MaintenanceCenter> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

    boolean existsByEmail(String email);
}
