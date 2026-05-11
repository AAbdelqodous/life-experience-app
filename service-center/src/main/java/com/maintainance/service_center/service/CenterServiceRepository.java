package com.maintainance.service_center.service;

import com.maintainance.service_center.category.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CenterServiceRepository extends JpaRepository<CenterService, Long> {

    List<CenterService> findByCenterIdAndIsActiveTrueOrderByCategoryIdAscServiceIdAsc(Long centerId);

    Optional<CenterService> findByCenterIdAndIdAndIsActiveTrue(Long centerId, Long id);

    boolean existsByCenterIdAndCategoryIdAndServiceIdAndIsActiveTrue(
            Long centerId, Long categoryId, Long serviceId);

    List<CenterService> findByCenterIdAndCategoryIdAndIsActiveTrueOrderByServiceIdAsc(
            Long centerId, Long categoryId);

    Optional<CenterService> findByCenterIdAndCategoryIdAndServiceId(
            Long centerId, Long categoryId, Long serviceId);

    Optional<CenterService> findByCenterIdAndCategoryIdAndServiceIdAndIsActiveTrue(
            Long centerId, Long categoryId, Long serviceId);

    @Query("SELECT DISTINCT cs.category FROM CenterService cs " +
           "WHERE cs.center.id = :centerId AND cs.isActive = true " +
           "ORDER BY cs.category.displayOrder ASC")
    List<ServiceCategory> findDistinctCategoriesByCenterId(@Param("centerId") Long centerId);
}
