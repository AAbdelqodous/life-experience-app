package com.maintainance.service_center.category;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceCategoryService {

    private final ServiceCategoryRepository categoryRepository;

    public List<ServiceCategoryResponse> findAllActive() {
        return categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Page<ServiceCategoryResponse> findAll(Pageable pageable) {
        return categoryRepository.findAll(pageable).map(this::toResponse);
    }

    public ServiceCategoryResponse findById(Long id) {
        return toResponse(getActiveCategory(id));
    }

    public ServiceCategoryResponse findByCode(String code) {
        ServiceCategory category = categoryRepository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with code: " + code));
        return toResponse(category);
    }

    @Transactional
    public ServiceCategoryResponse create(ServiceCategoryRequest request) {
        if (categoryRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("A category with this code already exists");
        }

        ServiceCategory parentCategory = null;
        if (request.getParentId() != null) {
            parentCategory = getActiveCategory(request.getParentId());
        }

        ServiceCategory category = ServiceCategory.builder()
                .code(request.getCode())
                .nameAr(request.getNameAr())
                .nameEn(request.getNameEn())
                .descriptionAr(request.getDescriptionAr())
                .descriptionEn(request.getDescriptionEn())
                .iconUrl(request.getIconUrl())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .isActive(true)
                .parent(parentCategory)
                .build();

        categoryRepository.save(category);
        log.info("Created service category id={} with code={}", category.getId(), category.getCode());
        return toResponse(category);
    }

    @Transactional
    public ServiceCategoryResponse update(Long id, ServiceCategoryRequest request) {
        ServiceCategory category = getActiveCategory(id);

        if (!category.getCode().equals(request.getCode()) && categoryRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("A category with this code already exists");
        }

        ServiceCategory parentCategory = null;
        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new IllegalArgumentException("A category cannot be its own parent");
            }
            parentCategory = getActiveCategory(request.getParentId());
        }

        category.setCode(request.getCode());
        category.setNameAr(request.getNameAr());
        category.setNameEn(request.getNameEn());
        category.setDescriptionAr(request.getDescriptionAr());
        category.setDescriptionEn(request.getDescriptionEn());
        category.setIconUrl(request.getIconUrl());
        category.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        category.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        category.setParent(parentCategory);

        categoryRepository.save(category);
        log.info("Updated service category id={}", id);
        return toResponse(category);
    }

    @Transactional
    public void deactivate(Long id) {
        ServiceCategory category = getActiveCategory(id);
        category.setIsActive(false);
        categoryRepository.save(category);
        log.info("Deactivated service category id={}", id);
    }

    private ServiceCategory getActiveCategory(Long id) {
        ServiceCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + id));
        if (!category.getIsActive()) {
            throw new EntityNotFoundException("Category not found with id: " + id);
        }
        return category;
    }

    private ServiceCategoryResponse toResponse(ServiceCategory category) {
        Long parentId = category.getParent() != null ? category.getParent().getId() : null;
        String parentNameAr = category.getParent() != null ? category.getParent().getNameAr() : null;
        String parentNameEn = category.getParent() != null ? category.getParent().getNameEn() : null;
        Integer subcategoryCount = category.getSubcategories() != null ? category.getSubcategories().size() : 0;
        Integer centerCount = category.getCenters() != null ? category.getCenters().size() : 0;

        return ServiceCategoryResponse.builder()
                .id(category.getId())
                .code(category.getCode())
                .nameAr(category.getNameAr())
                .nameEn(category.getNameEn())
                .descriptionAr(category.getDescriptionAr())
                .descriptionEn(category.getDescriptionEn())
                .iconUrl(category.getIconUrl())
                .displayOrder(category.getDisplayOrder())
                .isActive(category.getIsActive())
                .parentId(parentId)
                .parentNameAr(parentNameAr)
                .parentNameEn(parentNameEn)
                .subcategoryCount(subcategoryCount)
                .centerCount(centerCount)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedBy())
                .build();
    }
}
