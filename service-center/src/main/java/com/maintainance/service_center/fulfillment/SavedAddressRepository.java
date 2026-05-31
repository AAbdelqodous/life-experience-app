package com.maintainance.service_center.fulfillment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedAddressRepository extends JpaRepository<SavedAddress, Long> {

    List<SavedAddress> findByCustomerIdOrderByCreatedAtDesc(Integer customerId);

    Optional<SavedAddress> findByIdAndCustomerId(Long id, Integer customerId);
}
