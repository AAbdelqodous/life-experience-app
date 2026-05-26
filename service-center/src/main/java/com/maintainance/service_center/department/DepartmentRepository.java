package com.maintainance.service_center.department;

import com.maintainance.service_center.category.ServiceCategory;
import com.maintainance.service_center.center.MaintenanceCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    @Query("SELECT d FROM Department d WHERE d.center.id = :centerId " +
           "ORDER BY CASE WHEN d.isActive = true THEN 0 ELSE 1 END, d.displayOrder ASC, d.id ASC")
    List<Department> findByCenterIdActiveFirst(@Param("centerId") Long centerId);

    List<Department> findByCenterIdAndIsActiveTrueOrderByDisplayOrderAscIdAsc(Long centerId);

    Optional<Department> findByIdAndCenterId(Long id, Long centerId);

    @Query("SELECT d FROM Department d WHERE d.center.id = :centerId AND d.nameAr = :nameAr AND d.isActive = true")
    Optional<Department> findActiveByCenterIdAndNameAr(@Param("centerId") Long centerId, @Param("nameAr") String nameAr);

    @Query("SELECT d FROM Department d WHERE d.center.id = :centerId AND d.nameEn = :nameEn AND d.isActive = true")
    Optional<Department> findActiveByCenterIdAndNameEn(@Param("centerId") Long centerId, @Param("nameEn") String nameEn);

    @Query("SELECT COUNT(d) FROM Department d WHERE d.center.id = :centerId AND d.isActive = true")
    long countActiveByCenterId(@Param("centerId") Long centerId);

    @Query("SELECT d FROM Department d JOIN d.categories c " +
           "WHERE d.center = :center AND d.isActive = true AND c = :category " +
           "ORDER BY d.displayOrder ASC, d.id ASC")
    List<Department> findActiveByCenterAndCategory(@Param("center") MaintenanceCenter center,
                                                    @Param("category") ServiceCategory category);

    @Query("SELECT d FROM Department d WHERE d.center = :center AND d.isActive = true " +
           "ORDER BY d.displayOrder ASC, d.id ASC")
    List<Department> findActiveByCenter(@Param("center") MaintenanceCenter center);
}
