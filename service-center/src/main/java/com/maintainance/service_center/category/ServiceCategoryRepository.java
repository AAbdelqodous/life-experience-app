package com.maintainance.service_center.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {

    List<ServiceCategory> findByIsActiveTrueOrderByDisplayOrderAsc();

    Optional<ServiceCategory> findByCode(String code);

    boolean existsByCode(String code);
}
