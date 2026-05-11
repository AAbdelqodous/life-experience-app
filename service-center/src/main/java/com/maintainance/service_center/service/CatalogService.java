package com.maintainance.service_center.service;

import com.maintainance.service_center.category.ServiceCategory;
import com.maintainance.service_center.category.ServiceCategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final ServiceRepository serviceRepository;
    private final ServiceCategoryRepository categoryRepository;

    public List<ServiceResponse> findAllActive() {
        return serviceRepository.findByIsActiveTrueOrderByIdAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ServiceResponse> findByCategory(Long categoryId) {
        ServiceCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + categoryId));

        return category.getServices()
                .stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .map(this::toResponse)
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .toList();
    }

    private ServiceResponse toResponse(com.maintainance.service_center.service.Service svc) {
        return ServiceResponse.builder()
                .id(svc.getId())
                .code(svc.getCode())
                .nameAr(svc.getNameAr())
                .nameEn(svc.getNameEn())
                .descriptionAr(svc.getDescriptionAr())
                .descriptionEn(svc.getDescriptionEn())
                .iconUrl(svc.getIconUrl())
                .isActive(svc.getIsActive())
                .build();
    }
}
