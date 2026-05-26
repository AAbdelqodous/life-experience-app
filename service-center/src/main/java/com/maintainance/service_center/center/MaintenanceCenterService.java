package com.maintainance.service_center.center;

import com.maintainance.service_center.address.Address;
import com.maintainance.service_center.category.ServiceCategory;
import com.maintainance.service_center.category.ServiceCategoryRepository;
import com.maintainance.service_center.category.ServiceCategoryResponse;
import com.maintainance.service_center.config.FileStorageService;
import com.maintainance.service_center.service.CenterService;
import com.maintainance.service_center.service.CenterServiceRepository;
import com.maintainance.service_center.service.CenterServiceResponse;
import com.maintainance.service_center.service.ServiceManagementService;
import com.maintainance.service_center.staff.CenterMembership;
import com.maintainance.service_center.staff.CenterMembershipRepository;
import com.maintainance.service_center.staff.CenterRole;
import com.maintainance.service_center.staff.MembershipStatus;
import com.maintainance.service_center.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenanceCenterService {

    private final MaintenanceCenterRepository centerRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final FileStorageService fileStorageService;
    private final CenterMembershipRepository membershipRepository;
    private final CenterServiceRepository centerServiceRepository;
    private final ServiceManagementService serviceManagementService;

    @Transactional
    public MaintenanceCenterResponse create(MaintenanceCenterRequest request, User owner) {
        if (centerRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("A center with this email already exists");
        }

        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            log.warn("Phase 3.6: categoryIds in MaintenanceCenterRequest is deprecated; " +
                     "use POST /centers/my/services instead. Ignored for new center.");
        }

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
                .categories(new ArrayList<>())
                .owner(owner)
                .isVerified(false)
                .isActive(true)
                .build();

        centerRepository.save(center);

        CenterMembership ownerMembership = CenterMembership.builder()
                .user(owner)
                .center(center)
                .role(CenterRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .activatedAt(LocalDateTime.now())
                .build();
        membershipRepository.save(ownerMembership);

        log.info("Created maintenance center id={} by owner id={} with owner membership id={}",
                center.getId(), owner.getId(), ownerMembership.getId());
        return toResponse(center);
    }

    public Page<MaintenanceCenterSummaryResponse> findAll(Pageable pageable) {
        return centerRepository.findByIsActiveTrue(pageable).map(this::toSummaryResponse);
    }

    public MaintenanceCenterResponse findById(Long id) {
        return toResponse(getActiveCenter(id));
    }

    public MaintenanceCenterResponse findMyCenter(User owner) {
        return centerRepository.findByOwnerIdAndIsActiveTrue(owner.getId(), PageRequest.of(0, 1))
                .getContent()
                .stream()
                .findFirst()
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("No active center found for this owner"));
    }

    @Transactional
    public MaintenanceCenterResponse uploadImage(MultipartFile file, User owner) {
        MaintenanceCenter center = centerRepository.findByOwnerIdAndIsActiveTrue(owner.getId(), PageRequest.of(0, 1))
                .getContent()
                .stream()
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("No active center found for this owner"));

        String fileName = fileStorageService.storeFile(file);
        String imageUrl = "/uploads/" + fileName;

        if (center.getImageUrls() == null) {
            center.setImageUrls(new ArrayList<>());
        }
        center.getImageUrls().add(imageUrl);
        centerRepository.save(center);

        log.info("Image uploaded for center id={}", center.getId());
        return toResponse(center);
    }

    @Transactional
    public MaintenanceCenterResponse deleteImage(String imageUrl, User caller) {
        MaintenanceCenter center = centerRepository.findByOwnerIdAndIsActiveTrue(caller.getId(), PageRequest.of(0, 1))
                .getContent()
                .stream()
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("No active center found for this owner"));

        if (center.getImageUrls() == null || !center.getImageUrls().remove(imageUrl)) {
            throw new EntityNotFoundException("Image not found: " + imageUrl);
        }
        centerRepository.save(center);

        // Delete the physical file
        String fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
        fileStorageService.deleteFile(fileName);

        log.info("Image deleted for center id={}: {}", center.getId(), imageUrl);
        return toResponse(center);
    }

    @Transactional
    public MaintenanceCenterResponse uploadLogo(MultipartFile file, User owner) {
        MaintenanceCenter center = centerRepository.findByOwnerIdAndIsActiveTrue(owner.getId(), PageRequest.of(0, 1))
                .getContent()
                .stream()
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("No active center found for this owner"));

        if (center.getLogoUrl() != null) {
            String oldFileName = center.getLogoUrl().substring(center.getLogoUrl().lastIndexOf('/') + 1);
            fileStorageService.deleteFile(oldFileName);
        }

        String fileName = fileStorageService.storeFile(file);
        center.setLogoUrl("/uploads/" + fileName);
        centerRepository.save(center);

        log.info("Logo uploaded for center id={}", center.getId());
        return toResponse(center);
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

    public List<ServiceCategoryResponse> findCenterCategories(Long centerId) {
        getActiveCenter(centerId);
        return centerServiceRepository.findDistinctCategoriesByCenterId(centerId)
                .stream()
                .map(cat -> ServiceCategoryResponse.builder()
                        .id(cat.getId())
                        .code(cat.getCode())
                        .nameAr(cat.getNameAr())
                        .nameEn(cat.getNameEn())
                        .descriptionAr(cat.getDescriptionAr())
                        .descriptionEn(cat.getDescriptionEn())
                        .iconUrl(cat.getIconUrl())
                        .displayOrder(cat.getDisplayOrder())
                        .isActive(cat.getIsActive())
                        .build())
                .toList();
    }

    public List<CenterServiceResponse> findCenterCategoryServices(Long centerId, Long catId) {
        getActiveCenter(centerId);
        return centerServiceRepository
                .findByCenterIdAndCategoryIdAndIsActiveTrueOrderByServiceIdAsc(centerId, catId)
                .stream()
                .map(serviceManagementService::toResponse)
                .toList();
    }

    @Transactional
    public MaintenanceCenterResponse update(Long id, MaintenanceCenterRequest request, User caller) {
        MaintenanceCenter center = getActiveCenter(id);
        checkOwnership(center, caller);

        if (!Objects.equals(center.getEmail(), request.getEmail()) && centerRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("A center with this email already exists");
        }

        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            log.warn("Phase 3.6: categoryIds in MaintenanceCenterRequest is deprecated; " +
                     "use POST /centers/my/services instead. Ignored for center id={}", id);
        }

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
                .categories(deriveCategorySummaries(c))
                .specializations(c.getSpecializations())
                .logoUrl(c.getLogoUrl())
                .imageUrls(c.getImageUrls())
                .certifications(c.getCertifications())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private List<MaintenanceCenterResponse.CategorySummary> deriveCategorySummaries(MaintenanceCenter c) {
        List<CenterService> centerServices = c.getCenterServices();
        if (centerServices != null && !centerServices.isEmpty()) {
            return centerServices.stream()
                    .filter(cs -> Boolean.TRUE.equals(cs.getIsActive()))
                    .map(CenterService::getCategory)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted(Comparator.comparing(ServiceCategory::getDisplayOrder,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(cat -> MaintenanceCenterResponse.CategorySummary.builder()
                            .id(cat.getId())
                            .nameAr(cat.getNameAr())
                            .nameEn(cat.getNameEn())
                            .code(cat.getCode())
                            .build())
                    .toList();
        }
        return c.getCategories().stream()
                .map(cat -> MaintenanceCenterResponse.CategorySummary.builder()
                        .id(cat.getId())
                        .nameAr(cat.getNameAr())
                        .nameEn(cat.getNameEn())
                        .code(cat.getCode())
                        .build())
                .toList();
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
                .averageRating(c.getAverageRating() != null ? c.getAverageRating().doubleValue() : null)
                .totalReviews(c.getTotalReviews())
                .isVerified(c.getIsVerified())
                .isActive(c.getIsActive())
                .logoUrl(c.getLogoUrl())
                .workingDays(c.getWorkingDays())
                .build();
    }
}
