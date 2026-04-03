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

    java.util.Optional<MaintenanceCenter> findFirstByOwnerId(Integer ownerId);

    boolean existsByEmail(String email);

    // Search methods for advanced filtering
    @Query("SELECT c FROM MaintenanceCenter c WHERE c.isActive = true AND c.averageRating >= :minRating")
    Page<MaintenanceCenter> findByMinRating(@Param("minRating") Double minRating, Pageable pageable);

    @Query("SELECT c FROM MaintenanceCenter c WHERE c.isActive = true AND c.isVerified = :verified")
    Page<MaintenanceCenter> findByVerified(@Param("verified") Boolean verified, Pageable pageable);

    @Query("SELECT c FROM MaintenanceCenter c WHERE c.isActive = true AND " +
           "(:query IS NULL OR LOWER(c.nameAr) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.nameEn) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.descriptionAr) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.descriptionEn) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:categoryId IS NULL OR EXISTS (SELECT 1 FROM c.categories cat WHERE cat.id = :categoryId)) AND " +
           "(:minRating IS NULL OR c.averageRating >= :minRating) AND " +
           "(:verifiedOnly IS NULL OR c.isVerified = :verifiedOnly)")
    Page<MaintenanceCenter> searchCenters(
            @Param("query") String query,
            @Param("categoryId") Long categoryId,
            @Param("minRating") Double minRating,
            @Param("verifiedOnly") Boolean verifiedOnly,
            Pageable pageable);

    @Query(value = "SELECT c.* FROM maintenance_centers c WHERE c.is_active = true AND " +
            "(:latitude IS NULL OR :longitude IS NULL OR :maxDistance IS NULL OR " +
            "c.latitude IS NOT NULL AND c.longitude IS NOT NULL AND " +
            "6371 * acos(cos(radians(:latitude)) * cos(radians(c.latitude)) * " +
            "cos(radians(c.longitude) - radians(:longitude)) + " +
            "sin(radians(:latitude)) * sin(radians(c.latitude))) <= :maxDistance)",
            nativeQuery = true)
    Page<MaintenanceCenter> findByLocation(
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("maxDistance") Double maxDistance,
            Pageable pageable);

    @Query("SELECT c FROM MaintenanceCenter c WHERE c.isActive = true AND " +
           "(:query IS NULL OR LOWER(c.nameAr) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.nameEn) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:categoryId IS NULL OR EXISTS (SELECT 1 FROM c.categories cat WHERE cat.id = :categoryId)) AND " +
           "(:minRating IS NULL OR c.averageRating >= :minRating) AND " +
           "(:verifiedOnly IS NULL OR c.isVerified = :verifiedOnly) AND " +
           "(:latitude IS NULL OR :longitude IS NULL OR :maxDistance IS NULL OR " +
            "c.latitude IS NOT NULL AND c.longitude IS NOT NULL AND " +
            "6371 * acos(cos(radians(:latitude)) * cos(radians(c.latitude)) * " +
            "cos(radians(c.longitude) - radians(:longitude)) + " +
            "sin(radians(:latitude)) * sin(radians(c.latitude))) <= :maxDistance)")
    Page<MaintenanceCenter> searchCentersWithLocation(
            @Param("query") String query,
            @Param("categoryId") Long categoryId,
            @Param("minRating") Double minRating,
            @Param("verifiedOnly") Boolean verifiedOnly,
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("maxDistance") Double maxDistance,
            Pageable pageable);
}
