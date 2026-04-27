package com.maintainance.service_center.pricing;

import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.user.User;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CenterServicePricingService {
    
    private final CenterServicePricingRepository pricingRepository;
    private final MaintenanceCenterRepository centerRepository;
    
    public List<CenterServicePricingResponse> getMyPricing(User owner) {
        MaintenanceCenter center = getOwnerCenter(owner);
        List<CenterServicePricing> pricingList = pricingRepository.findByCenterIdOrderByCreatedAtDesc(center.getId());
        return pricingList.stream()
                .map(CenterServicePricingResponse::from)
                .toList();
    }

    @Transactional
    public CenterServicePricingResponse createPricing(User owner, CenterServicePricingRequest request) {
        if (request.getMaxPrice().compareTo(request.getMinPrice()) < 0) {
            throw new IllegalArgumentException("Maximum price must be greater than or equal to minimum price");
        }

        MaintenanceCenter center = getOwnerCenter(owner);

        if (pricingRepository.existsByCenterIdAndServiceTypeAndServiceNameEn(
                center.getId(), request.getServiceType(), request.getServiceNameEn())) {
            throw new IllegalStateException("A pricing entry for service type " + request.getServiceType()
                    + " with name '" + request.getServiceNameEn() + "' already exists for this center");
        }
        
        CenterServicePricing pricing = new CenterServicePricing();
        pricing.setCenter(center);
        pricing.setServiceType(request.getServiceType());
        pricing.setServiceNameAr(request.getServiceNameAr());
        pricing.setServiceNameEn(request.getServiceNameEn());
        pricing.setMinPrice(request.getMinPrice());
        pricing.setMaxPrice(request.getMaxPrice());
        pricing.setTypicalDurationMinutes(request.getTypicalDurationMinutes());
        pricing.setDescriptionAr(request.getDescriptionAr());
        pricing.setDescriptionEn(request.getDescriptionEn());
        pricing.setActive(true);
        
        CenterServicePricing saved = pricingRepository.save(pricing);
        log.info("Created pricing entry for center {} and service type {}", center.getId(), request.getServiceType());
        return CenterServicePricingResponse.from(saved);
    }
    
    @Transactional
    public CenterServicePricingResponse updatePricing(User owner, Long id, CenterServicePricingRequest request) {
        if (request.getMaxPrice().compareTo(request.getMinPrice()) < 0) {
            throw new IllegalArgumentException("Maximum price must be greater than or equal to minimum price");
        }

        MaintenanceCenter center = getOwnerCenter(owner);

        if (pricingRepository.existsByCenterIdAndServiceTypeAndServiceNameEnAndIdNot(
                center.getId(), request.getServiceType(), request.getServiceNameEn(), id)) {
            throw new IllegalStateException("A pricing entry for service type " + request.getServiceType()
                    + " with name '" + request.getServiceNameEn() + "' already exists for this center");
        }

        CenterServicePricing pricing = pricingRepository.findByIdAndCenterId(id, center.getId())
                .orElseThrow(() -> new EntityNotFoundException("Pricing entry not found with id: " + id));
        
        pricing.setServiceType(request.getServiceType());
        pricing.setServiceNameAr(request.getServiceNameAr());
        pricing.setServiceNameEn(request.getServiceNameEn());
        pricing.setMinPrice(request.getMinPrice());
        pricing.setMaxPrice(request.getMaxPrice());
        pricing.setTypicalDurationMinutes(request.getTypicalDurationMinutes());
        pricing.setDescriptionAr(request.getDescriptionAr());
        pricing.setDescriptionEn(request.getDescriptionEn());
        
        if (request.getIsActive() != null) {
            pricing.setActive(request.getIsActive());
        }
        
        CenterServicePricing saved = pricingRepository.save(pricing);
        log.info("Updated pricing entry {} for center {}", id, center.getId());
        return CenterServicePricingResponse.from(saved);
    }
    
    @Transactional
    public void deletePricing(User owner, Long id) {
        MaintenanceCenter center = getOwnerCenter(owner);
        
        CenterServicePricing pricing = pricingRepository.findByIdAndCenterId(id, center.getId())
                .orElseThrow(() -> new EntityNotFoundException("Pricing entry not found with id: " + id));
        
        pricingRepository.delete(pricing);
        log.info("Deleted pricing entry {} for center {}", id, center.getId());
    }
    
    private MaintenanceCenter getOwnerCenter(User owner) {
        return centerRepository.findFirstByOwnerId(owner.getId())
                .orElseThrow(() -> new EntityNotFoundException("Maintenance center not found for owner"));
    }
}