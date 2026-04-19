package com.maintainance.service_center.pricing;

import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.user.User;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CenterServicePricingService {
    
    private final CenterServicePricingRepository pricingRepository;
    private final MaintenanceCenterRepository centerRepository;
    
    public List<CenterServicePricingResponse> getMyPricing(User owner) {
        MaintenanceCenter center = getCenterForOwner(owner);
        List<CenterServicePricing> pricingList = pricingRepository.findByCenterIdOrderByCreatedAtDesc(center.getId());
        return pricingList.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public CenterServicePricingResponse createPricing(User owner, CreatePricingRequest request) {
        // Validate maxPrice >= minPrice
        if (request.getMaxPrice().compareTo(request.getMinPrice()) < 0) {
            throw new IllegalArgumentException("Maximum price must be greater than or equal to minimum price");
        }
        
        MaintenanceCenter center = getCenterForOwner(owner);
        
        // Validate no existing active entry for same serviceType in this center
        if (pricingRepository.existsByCenterIdAndServiceType(center.getId(), request.getServiceType())) {
            throw new IllegalStateException("A pricing entry for service type " + request.getServiceType() + 
                    " already exists for this center");
        }
        
        CenterServicePricing pricing = CenterServicePricing.builder()
                .center(center)
                .serviceType(request.getServiceType())
                .serviceNameAr(request.getServiceNameAr())
                .serviceNameEn(request.getServiceNameEn())
                .minPrice(request.getMinPrice())
                .maxPrice(request.getMaxPrice())
                .typicalDurationMinutes(request.getTypicalDurationMinutes())
                .descriptionAr(request.getDescriptionAr())
                .descriptionEn(request.getDescriptionEn())
                .isActive(true)
                .build();
        
        CenterServicePricing saved = pricingRepository.save(pricing);
        log.info("Created pricing entry for center {} and service type {}", center.getId(), request.getServiceType());
        return mapToResponse(saved);
    }
    
    @Transactional
    public CenterServicePricingResponse updatePricing(User owner, Long id, UpdatePricingRequest request) {
        // Validate maxPrice >= minPrice
        if (request.getMaxPrice().compareTo(request.getMinPrice()) < 0) {
            throw new IllegalArgumentException("Maximum price must be greater than or equal to minimum price");
        }
        
        MaintenanceCenter center = getCenterForOwner(owner);
        
        // Validate no existing active entry for same serviceType in this center (excluding current id)
        if (pricingRepository.existsByCenterIdAndServiceTypeAndIdNot(
                center.getId(), request.getServiceType(), id)) {
            throw new IllegalStateException("A pricing entry for service type " + request.getServiceType() + 
                    " already exists for this center");
        }
        
        CenterServicePricing pricing = pricingRepository.findByCenterIdAndId(center.getId(), id)
                .orElseThrow(() -> new EntityNotFoundException("Pricing entry not found with id: " + id));
        
        pricing.setServiceType(request.getServiceType());
        pricing.setServiceNameAr(request.getServiceNameAr());
        pricing.setServiceNameEn(request.getServiceNameEn());
        pricing.setMinPrice(request.getMinPrice());
        pricing.setMaxPrice(request.getMaxPrice());
        pricing.setTypicalDurationMinutes(request.getTypicalDurationMinutes());
        pricing.setDescriptionAr(request.getDescriptionAr());
        pricing.setDescriptionEn(request.getDescriptionEn());
        pricing.setIsActive(request.getIsActive());
        
        CenterServicePricing saved = pricingRepository.save(pricing);
        log.info("Updated pricing entry {} for center {}", id, center.getId());
        return mapToResponse(saved);
    }
    
    @Transactional
    public void deletePricing(User owner, Long id) {
        MaintenanceCenter center = getCenterForOwner(owner);
        
        CenterServicePricing pricing = pricingRepository.findByCenterIdAndId(center.getId(), id)
                .orElseThrow(() -> new EntityNotFoundException("Pricing entry not found with id: " + id));
        
        pricingRepository.delete(pricing);
        log.info("Deleted pricing entry {} for center {}", id, center.getId());
    }
    
    private MaintenanceCenter getCenterForOwner(User owner) {
        return centerRepository.findFirstByOwnerId(owner.getId())
                .orElseThrow(() -> new EntityNotFoundException("Maintenance center not found for owner"));
    }
    
    private CenterServicePricingResponse mapToResponse(CenterServicePricing pricing) {
        return CenterServicePricingResponse.builder()
                .id(pricing.getId())
                .serviceType(pricing.getServiceType())
                .serviceNameAr(pricing.getServiceNameAr())
                .serviceNameEn(pricing.getServiceNameEn())
                .minPrice(pricing.getMinPrice())
                .maxPrice(pricing.getMaxPrice())
                .typicalDurationMinutes(pricing.getTypicalDurationMinutes())
                .descriptionAr(pricing.getDescriptionAr())
                .descriptionEn(pricing.getDescriptionEn())
                .isActive(pricing.getIsActive())
                .createdAt(pricing.getCreatedAt())
                .updatedAt(pricing.getUpdatedAt())
                .build();
    }
}