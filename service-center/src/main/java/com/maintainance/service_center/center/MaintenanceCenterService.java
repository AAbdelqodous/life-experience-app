package com.maintainance.service_center.center;

import com.maintainance.service_center.address.Address;
import com.maintainance.service_center.category.ServiceCategory;
import com.maintainance.service_center.category.ServiceCategoryRepository;
import com.maintainance.service_center.config.FileStorageService;
import com.maintainance.service_center.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenanceCenterService {

    private final MaintenanceCenterRepository centerRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public MaintenanceCenterResponse create(MaintenanceCenterRequest request, User owner) {
        if (centerRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("A center with this email already exists");
        }

        List<ServiceCategory> categories = resolveCategories(request.getCategoryIds());

        MaintenanceCenter center = MaintenanceCenter.builder()
                .nameAr(request.getNameAr())
                .nameEn(request.getNameEn())
                .descriptionAr(request.getDescriptionAr())
                .descriptionEn(request.getDescriptionEn())
                .email(request.getEmail())
                .phone(request.getPhone())
                .alternativePhone(request.getAlternativePhone())
                .address(mapAddress(request.getAddress()))
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .openingTime(request.getOpeningTime())
                .closingTime(request.getClosingTime())
                .workingDays(request.getWorkingDays() != null ? request.getWorkingDays() : new ArrayList<>())
                .specializations(request.getSpecializations() != null ? request.getSpecializations() : new ArrayList<>())
                .logoUrl(request.getLogoUrl())
                .imageUrls(request.getImageUrls() != null ? request.getImageUrls() : new ArrayList<>())
                .certifications(request.getCertifications() != null ? request.getCertifications() : new ArrayList<>())
                .categories(categories)
                .owner(owner)
                .isVerified(false)
                .isActive(true)
                .build();

        centerRepository.save(center);
        log.info("Created maintenance center id={} by owner id={}", center.getId(), owner.getId());
        return toResponse(center);
    }

    public Page<MaintenanceCenterSummaryResponse> findAll(Pageable pageable) {
        return centerRepository.findByIsActiveTrue(pageable).map(this::toSummaryResponse);
    }

    public MaintenanceCenterResponse findById(Long id) {
        return toResponse(getActiveCenter(id));
    }

    public Page<MaintenanceCenterSummaryResponse> findByOwner(User owner, Pageable pageable) {
        return centerRepository.findByOwnerIdAndIsActiveTrue(owner.getId(), pageable).map(this::toSummaryResponse);
    }

    public MaintenanceCenterResponse getMyCenterProfile(User owner) {
        MaintenanceCenter center = centerRepository.findFirstByOwnerId(owner.getId())
                .orElseThrow(() -> new EntityNotFoundException("No center found for this account"));
        return toResponse(center);
    }

    public Page<MaintenanceCenterSummaryResponse> search(String query, Pageable pageable) {
        return centerRepository.searchByName(query, pageable).map(this::toSummaryResponse);
    }

    public Page<MaintenanceCenterSummaryResponse> findByCategory(Long categoryId, Pageable pageable) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new EntityNotFoundException("Category not found with id: " + categoryId);
        }
        return centerRepository.findByCategoryId(categoryId, pageable).map(this::toSummaryResponse);
    }

    @Transactional
    public MaintenanceCenterResponse update(Long id, MaintenanceCenterRequest request, User caller) {
        MaintenanceCenter center = getActiveCenter(id);
        checkOwnership(center, caller);

        if (!Objects.equals(center.getEmail(), request.getEmail()) && centerRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("A center with this email already exists");
        }

        List<ServiceCategory> categories = resolveCategories(request.getCategoryIds());

        center.setNameAr(request.getNameAr());
        center.setNameEn(request.getNameEn());
        center.setDescriptionAr(request.getDescriptionAr());
        center.setDescriptionEn(request.getDescriptionEn());
        center.setEmail(request.getEmail());
        center.setPhone(request.getPhone());
        center.setAlternativePhone(request.getAlternativePhone());
        center.setAddress(mapAddress(request.getAddress()));
        center.setLatitude(request.getLatitude());
        center.setLongitude(request.getLongitude());
        center.setOpeningTime(request.getOpeningTime());
        center.setClosingTime(request.getClosingTime());
        if (request.getIsActive() != null) center.setIsActive(request.getIsActive());
        center.setWorkingDays(request.getWorkingDays() != null ? request.getWorkingDays() : new ArrayList<>());
        center.setSpecializations(request.getSpecializations() != null ? request.getSpecializations() : new ArrayList<>());
        center.setLogoUrl(request.getLogoUrl());
        center.setImageUrls(request.getImageUrls() != null ? request.getImageUrls() : new ArrayList<>());
        center.setCertifications(request.getCertifications() != null ? request.getCertifications() : new ArrayList<>());
        center.setCategories(categories);

        centerRepository.save(center);
        log.info("Updated maintenance center id={}", id);
        return toResponse(center);
    }

    @Transactional
    public MaintenanceCenterResponse updateMy(MaintenanceCenterRequest request, User caller) {
        MaintenanceCenter center = centerRepository.findFirstByOwnerId(caller.getId())
                .orElseThrow(() -> new EntityNotFoundException("No center found for this account"));
        return update(center.getId(), request, caller);
    }

    @Transactional
    public MaintenanceCenterResponse addImages(MultipartFile file, User caller) {
        MaintenanceCenter center = centerRepository.findFirstByOwnerId(caller.getId())
                .orElseThrow(() -> new EntityNotFoundException("No center found for this account"));
        String fileName = fileStorageService.storeFile(file);
        String imageUrl = "/uploads/" + fileName;
        List<String> updated = new ArrayList<>(center.getImageUrls());
        updated.add(imageUrl);
        center.setImageUrls(updated);
        centerRepository.save(center);
        log.info("Added image {} to center id={}", imageUrl, center.getId());
        return toResponse(center);
    }

    @Transactional
    public void deactivate(Long id, User caller) {
        MaintenanceCenter center = getActiveCenter(id);
        checkOwnership(center, caller);
        center.setIsActive(false);
        centerRepository.save(center);
        log.info("Deactivated maintenance center id={}", id);
    }

    private MaintenanceCenter getActiveCenter(Long id) {
        MaintenanceCenter center = centerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Center not found with id: " + id));
        if (!center.getIsActive()) {
            throw new EntityNotFoundException("Center not found with id: " + id);
        }
        return center;
    }

    private void checkOwnership(MaintenanceCenter center, User caller) {
        if (!center.getOwner().getId().equals(caller.getId())) {
            throw new AccessDeniedException("You do not have permission to modify this center");
        }
    }

    private List<ServiceCategory> resolveCategories(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new IllegalArgumentException("At least one category is required");
        }
        List<ServiceCategory> categories = categoryRepository.findAllById(categoryIds);
        if (categories.size() != categoryIds.size()) {
            throw new EntityNotFoundException("One or more categories not found");
        }
        return categories;
    }

    private Address mapAddress(com.maintainance.service_center.address.AddressRequest req) {
        if (req == null) return null;
        Address address = new Address();
        address.setStreetAr(req.getStreetAr());
        address.setStreetEn(req.getStreetEn());
        address.setDistrictAr(req.getDistrictAr());
        address.setDistrictEn(req.getDistrictEn());
        address.setCityAr(req.getCityAr());
        address.setCityEn(req.getCityEn());
        address.setGovernorateAr(req.getGovernorateAr());
        address.setGovernorateEn(req.getGovernorateEn());
        address.setPostalCode(req.getPostalCode());
        address.setBuildingNumber(req.getBuildingNumber());
        address.setFloor(req.getFloor());
        address.setLandMark(req.getLandMark());
        return address;
    }

    private MaintenanceCenterResponse toResponse(MaintenanceCenter c) {
        return MaintenanceCenterResponse.builder()
                .id(c.getId())
                .nameAr(c.getNameAr())
                .nameEn(c.getNameEn())
                .descriptionAr(c.getDescriptionAr())
                .descriptionEn(c.getDescriptionEn())
                .email(c.getEmail())
                .phone(c.getPhone())
                .alternativePhone(c.getAlternativePhone())
                .address(c.getAddress())
                .latitude(c.getLatitude())
                .longitude(c.getLongitude())
                .openingTime(c.getOpeningTime())
                .closingTime(c.getClosingTime())
                .workingDays(c.getWorkingDays())
                .averageRating(c.getAverageRating())
                .totalReviews(c.getTotalReviews())
                .isVerified(c.getIsVerified())
                .isActive(c.getIsActive())
                .ownerId(c.getOwner().getId())
                .ownerName(c.getOwner().fullName())
                .categories(c.getCategories().stream().map(cat ->
                        MaintenanceCenterResponse.CategorySummary.builder()
                                .id(cat.getId())
                                .nameAr(cat.getNameAr())
                                .nameEn(cat.getNameEn())
                                .code(cat.getCode())
                                .build()
                ).toList())
                .specializations(c.getSpecializations())
                .logoUrl(c.getLogoUrl())
                .imageUrls(c.getImageUrls())
                .certifications(c.getCertifications())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private MaintenanceCenterSummaryResponse toSummaryResponse(MaintenanceCenter c) {
        String cityAr = c.getAddress() != null ? c.getAddress().getCityAr() : null;
        String cityEn = c.getAddress() != null ? c.getAddress().getCityEn() : null;
        return MaintenanceCenterSummaryResponse.builder()
                .id(c.getId())
                .nameAr(c.getNameAr())
                .nameEn(c.getNameEn())
                .cityAr(cityAr)
                .cityEn(cityEn)
                .averageRating(c.getAverageRating())
                .totalReviews(c.getTotalReviews())
                .isVerified(c.getIsVerified())
                .logoUrl(c.getLogoUrl())
                .workingDays(c.getWorkingDays())
                .build();
    }
}
