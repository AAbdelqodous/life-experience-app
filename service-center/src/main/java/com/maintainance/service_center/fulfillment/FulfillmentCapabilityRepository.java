package com.maintainance.service_center.fulfillment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FulfillmentCapabilityRepository extends JpaRepository<FulfillmentCapability, Long> {

    Optional<FulfillmentCapability> findByCenterId(Long centerId);
}
