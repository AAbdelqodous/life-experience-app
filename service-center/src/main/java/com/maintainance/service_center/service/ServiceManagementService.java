package com.maintainance.service_center.service;

import com.maintainance.service_center.category.ServiceCategory;
import com.maintainance.service_center.category.ServiceCategoryRepository;
import com.maintainance.service_center.center.CenterResolverService;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class ServiceManagementService {

    private final CenterServiceRepository centerServiceRepository;
    private final CenterResolverService centerResolver;
    private final ServiceCategoryRepository categoryRepository;
    private final ServiceRepository serviceRepository;

    public List<CenterServiceResponse> listForOwner(User owner) {
        MaintenanceCenter center = centerResolver.resolveCenter(owner);
        return centerServiceRepository
                .findByCenterIdAndIsActiveTrueOrderByCategoryIdAscServiceIdAsc(center.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CenterServiceResponse create(CreateCenterServiceRequest request, User owner) {
        MaintenanceCenter center = centerResolver.resolveCenter(owner);

        ServiceCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Category not found: " + request.getCategoryId()));

        Service service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Service not found: " + request.getServiceId()));

        boolean validPair = category.getServices().stream()
                .anyMatch(s -> s.getId().equals(request.getServiceId()));
        if (!validPair) {
            throw new IllegalArgumentException(
                    "Service '" + service.getCode() + "' is not available for category '" +
                    category.getCode() + "'");
        }

        validatePrices(request.getMinPrice(), request.getMaxPrice());

        // Re-activate if previously soft-deleted (unique constraint prevents a second INSERT)
        Optional<CenterService> existing = centerServiceRepository
                .findByCenterIdAndCategoryIdAndServiceId(
                        center.getId(), request.getCategoryId(), request.getServiceId());

        if (existing.isPresent()) {
            CenterService cs = existing.get();
            if (Boolean.TRUE.equals(cs.getIsActive())) {
                throw new IllegalStateException(
                        "This center already offers '" + service.getCode() +
                        "' under category '" + category.getCode() + "'");
            }
            cs.setIsActive(true);
            cs.setMinPrice(request.getMinPrice());
            cs.setMaxPrice(request.getMaxPrice());
            cs.setTypicalDurationMinutes(request.getTypicalDurationMinutes());
            cs.setDescriptionAr(request.getDescriptionAr());
            cs.setDescriptionEn(request.getDescriptionEn());
            centerServiceRepository.save(cs);
            log.info("Re-activated center service id={} for center id={}", cs.getId(), center.getId());
            return toResponse(cs);
        }

        CenterService centerService = CenterService.builder()
                .center(center)
                .category(category)
                .service(service)
                .minPrice(request.getMinPrice())
                .maxPrice(request.getMaxPrice())
                .typicalDurationMinutes(request.getTypicalDurationMinutes())
                .descriptionAr(request.getDescriptionAr())
                .descriptionEn(request.getDescriptionEn())
                .isActive(true)
                .build();

        centerServiceRepository.save(centerService);
        log.info("Created center service id={} for center id={}", centerService.getId(), center.getId());
        return toResponse(centerService);
    }

    @Transactional
    public CenterServiceResponse update(Long id, UpdateCenterServiceRequest request, User owner) {
        MaintenanceCenter center = centerResolver.resolveCenter(owner);

        CenterService cs = centerServiceRepository
                .findByCenterIdAndIdAndIsActiveTrue(center.getId(), id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Service offering not found with id: " + id));

        validatePrices(request.getMinPrice(), request.getMaxPrice());

        if (request.getMinPrice() != null)              cs.setMinPrice(request.getMinPrice());
        if (request.getMaxPrice() != null)              cs.setMaxPrice(request.getMaxPrice());
        if (request.getTypicalDurationMinutes() != null) cs.setTypicalDurationMinutes(request.getTypicalDurationMinutes());
        if (request.getDescriptionAr() != null)         cs.setDescriptionAr(request.getDescriptionAr());
        if (request.getDescriptionEn() != null)         cs.setDescriptionEn(request.getDescriptionEn());

        centerServiceRepository.save(cs);
        log.info("Updated center service id={} for center id={}", id, center.getId());
        return toResponse(cs);
    }

    @Transactional
    public void delete(Long id, User owner) {
        MaintenanceCenter center = centerResolver.resolveCenter(owner);

        CenterService cs = centerServiceRepository
                .findByCenterIdAndIdAndIsActiveTrue(center.getId(), id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Service offering not found with id: " + id));

        cs.setIsActive(false);
        centerServiceRepository.save(cs);
        log.info("Soft-deleted center service id={} for center id={}", id, center.getId());
    }

    private void validatePrices(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && maxPrice == null) {
            throw new IllegalArgumentException("maxPrice is required when minPrice is provided");
        }
        if (minPrice != null && maxPrice.compareTo(minPrice) < 0) {
            throw new IllegalArgumentException("maxPrice must be greater than or equal to minPrice");
        }
    }

    public CenterServiceResponse toResponse(CenterService cs) {
        return CenterServiceResponse.builder()
                .id(cs.getId())
                .category(CenterServiceResponse.CategoryInfo.builder()
                        .id(cs.getCategory().getId())
                        .code(cs.getCategory().getCode())
                        .nameAr(cs.getCategory().getNameAr())
                        .nameEn(cs.getCategory().getNameEn())
                        .build())
                .service(CenterServiceResponse.ServiceInfo.builder()
                        .id(cs.getService().getId())
                        .code(cs.getService().getCode())
                        .nameAr(cs.getService().getNameAr())
                        .nameEn(cs.getService().getNameEn())
                        .build())
                .minPrice(cs.getMinPrice())
                .maxPrice(cs.getMaxPrice())
                .typicalDurationMinutes(cs.getTypicalDurationMinutes())
                .descriptionAr(cs.getDescriptionAr())
                .descriptionEn(cs.getDescriptionEn())
                .isActive(cs.getIsActive())
                .createdAt(cs.getCreatedAt())
                .build();
    }
}
