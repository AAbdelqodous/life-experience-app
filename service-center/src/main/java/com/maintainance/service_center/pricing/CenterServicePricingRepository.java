package com.maintainance.service_center.pricing;

import com.maintainance.service_center.booking.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CenterServicePricingRepository extends JpaRepository<CenterServicePricing, Long> {

    List<CenterServicePricing> findByCenterIdOrderByCreatedAtDesc(Long centerId);

    List<CenterServicePricing> findByCenterIdAndIsActiveTrue(Long centerId);

    Optional<CenterServicePricing> findByIdAndCenterId(Long id, Long centerId);

    boolean existsByCenterIdAndServiceTypeAndServiceNameEn(Long centerId, ServiceType serviceType, String serviceNameEn);

    boolean existsByCenterIdAndServiceTypeAndServiceNameEnAndIdNot(Long centerId, ServiceType serviceType, String serviceNameEn, Long id);
}