package com.maintainance.service_center.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedMethodRepository extends JpaRepository<SavedMethod, Long> {
    List<SavedMethod> findByCustomerId(Integer customerId);
    Optional<SavedMethod> findByIdAndCustomerId(Long id, Integer customerId);
}
