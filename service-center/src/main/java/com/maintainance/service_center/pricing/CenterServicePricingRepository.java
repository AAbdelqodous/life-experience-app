package com.maintainance.service_center.pricing;

import com.maintainance.service_center.booking.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CenterServicePricingRepository extends JpaRepository<CenterServicePricing, Long> {
    
    List<CenterServicePricing> findByCenterIdOrderByCreatedAtDesc(Long centerId);
    
    Optional<CenterServicePricing> findByCenterIdAndId(Long centerId, Long id);
    
    boolean existsByCenterIdAndServiceTypeAndIdNot(Long centerId, ServiceType serviceType, Long excludeId);
    
    boolean existsByCenterIdAndServiceType(Long centerId, ServiceType serviceType);
}