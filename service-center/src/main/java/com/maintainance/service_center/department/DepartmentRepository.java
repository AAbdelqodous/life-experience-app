package com.maintainance.service_center.department;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findByCenterIdAndIsActiveTrue(Long centerId);

    List<Department> findByCenterIdOrderByDisplayOrderAsc(Long centerId);

    Optional<Department> findByIdAndCenterId(Long id, Long centerId);

    boolean existsByCenterIdAndNameAr(Long centerId, String nameAr);

    boolean existsByCenterIdAndNameEn(Long centerId, String nameEn);

    @Query("SELECT COUNT(d) FROM Department d WHERE d.center.id = :centerId AND d.isActive = true")
    long countActiveByCenterId(@Param("centerId") Long centerId);
}
