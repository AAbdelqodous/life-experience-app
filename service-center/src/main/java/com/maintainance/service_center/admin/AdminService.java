package com.maintainance.service_center.admin;

import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.booking.BookingStatus;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.center.MaintenanceCenterResponse;
import com.maintainance.service_center.category.ServiceCategory;
import com.maintainance.service_center.category.ServiceCategoryRepository;
import com.maintainance.service_center.category.ServiceCategoryRequest;
import com.maintainance.service_center.category.ServiceCategoryResponse;
import com.maintainance.service_center.complaint.Complaint;
import com.maintainance.service_center.complaint.ComplaintPriority;
import com.maintainance.service_center.complaint.ComplaintRepository;
import com.maintainance.service_center.complaint.ComplaintResponse;
import com.maintainance.service_center.complaint.ComplaintStatus;
import com.maintainance.service_center.complaint.ComplaintType;
import com.maintainance.service_center.user.ApprovalStatus;
import com.maintainance.service_center.user.User;
import com.maintainance.service_center.user.UserRepository;
import com.maintainance.service_center.user.UserResponse;
import com.maintainance.service_center.user.UserService;
import com.maintainance.service_center.user.UserType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final MaintenanceCenterRepository centerRepository;
    private final BookingRepository bookingRepository;
    private final ComplaintRepository complaintRepository;
    private final ServiceCategoryRepository categoryRepository;

    public Page<UserResponse> getPendingOwners(Pageable pageable) {
        return userRepository
                .findByUserTypeAndApprovalStatus(UserType.CENTER_OWNER, ApprovalStatus.PENDING_APPROVAL, pageable)
                .map(userService::toResponse);
    }

    public Page<UserResponse> getAllUsers(UserType type, Pageable pageable) {
        Page<User> page = (type != null)
                ? userRepository.findByUserType(type, pageable)
                : userRepository.findAll(pageable);
        return page.map(userService::toResponse);
    }

    @Transactional
    public UserResponse approveOwner(Integer userId) {
        User user = findOwner(userId);
        user.setApprovalStatus(ApprovalStatus.APPROVED);
        userRepository.save(user);
        log.info("Admin approved center owner ID: {}", userId);
        return userService.toResponse(user);
    }

    @Transactional
    public UserResponse rejectOwner(Integer userId, AdminRejectRequest request) {
        User user = findOwner(userId);
        user.setApprovalStatus(ApprovalStatus.REJECTED);
        if (request != null && request.getReason() != null) {
            user.setRejectionReason(request.getReason());
        }
        userRepository.save(user);
        log.info("Admin rejected center owner ID: {}", userId);
        return userService.toResponse(user);
    }

    private User findOwner(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        if (user.getUserType() != UserType.CENTER_OWNER) {
            throw new IllegalArgumentException("User " + userId + " is not a center owner");
        }
        return user;
    }

    // Center management methods
    public Page<AdminCenterResponse> getAllCenters(ApprovalStatus approvalStatus, Boolean enabled, Pageable pageable) {
        Page<MaintenanceCenter> centers;
        if (approvalStatus != null && enabled != null) {
            centers = centerRepository.findByOwnerApprovalStatusAndEnabled(approvalStatus, enabled, pageable);
        } else if (approvalStatus != null) {
            centers = centerRepository.findByOwnerApprovalStatus(approvalStatus, pageable);
        } else if (enabled != null) {
            centers = centerRepository.findByEnabled(enabled, pageable);
        } else {
            centers = centerRepository.findAll(pageable);
        }
        return centers.map(this::toAdminCenterResponse);
    }

    public AdminCenterResponse getCenterById(Long id) {
        MaintenanceCenter center = centerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Center not found: " + id));
        return toAdminCenterResponse(center);
    }

    @Transactional
    public AdminCenterResponse enableCenter(Long id) {
        MaintenanceCenter center = centerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Center not found: " + id));
        center.setEnabled(true);
        center = centerRepository.save(center);
        log.info("Admin enabled center ID: {}", id);
        return toAdminCenterResponse(center);
    }

    @Transactional
    public AdminCenterResponse disableCenter(Long id) {
        MaintenanceCenter center = centerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Center not found: " + id));
        center.setEnabled(false);
        center = centerRepository.save(center);
        log.info("Admin disabled center ID: {}", id);
        return toAdminCenterResponse(center);
    }

    // Platform-wide bookings view
    public Page<AdminBookingResponse> getAllBookings(BookingStatus status, Long centerId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Page<Booking> bookings = bookingRepository.findAllWithFilters(status, centerId, startDate, endDate, pageable);
        return bookings.map(this::toAdminBookingResponse);
    }

    private static final Set<ComplaintStatus> OPEN_STATUSES = Set.of(
            ComplaintStatus.PENDING,
            ComplaintStatus.UNDER_REVIEW,
            ComplaintStatus.IN_PROGRESS,
            ComplaintStatus.ESCALATED
    );

    // Complaint management methods
    public Page<ComplaintResponse> getAllComplaints(ComplaintStatus status, ComplaintType type, ComplaintPriority priority, Pageable pageable) {
        Page<Complaint> page = complaintRepository.findAllWithFilters(status, type, priority, pageable);
        List<Complaint> sorted = page.getContent().stream()
                .sorted(Comparator
                        .comparingInt((Complaint c) -> OPEN_STATUSES.contains(c.getStatus()) ? 0 : 1)
                        .thenComparing(Comparator.comparing(Complaint::getCreatedAt).reversed()))
                .collect(Collectors.toList());
        return new PageImpl<>(sorted, pageable, page.getTotalElements())
                .map(this::toAdminComplaintResponse);
    }

    @Transactional
    public ComplaintResponse updateComplaintStatus(Long id, AdminComplaintStatusRequest request) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Complaint not found: " + id));
        complaint.setStatus(request.getStatus());
        complaint = complaintRepository.save(complaint);
        log.info("Admin updated complaint ID {} status to {}", id, request.getStatus());
        return toAdminComplaintResponse(complaint);
    }

    @Transactional
    public ComplaintResponse updateComplaintPriority(Long id, AdminComplaintPriorityRequest request) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Complaint not found: " + id));
        complaint.setPriority(request.getPriority());
        complaint = complaintRepository.save(complaint);
        log.info("Admin updated complaint ID {} priority to {}", id, request.getPriority());
        return toAdminComplaintResponse(complaint);
    }

    private AdminCenterResponse toAdminCenterResponse(MaintenanceCenter center) {
        List<MaintenanceCenterResponse.CategorySummary> categories = center.getCategories().stream()
                .map(cat -> MaintenanceCenterResponse.CategorySummary.builder()
                        .id(cat.getId())
                        .nameAr(cat.getNameAr())
                        .nameEn(cat.getNameEn())
                        .code(cat.getCode())
                        .build())
                .collect(Collectors.toList());

        return AdminCenterResponse.builder()
                .id(center.getId())
                .nameAr(center.getNameAr())
                .nameEn(center.getNameEn())
                .descriptionAr(center.getDescriptionAr())
                .descriptionEn(center.getDescriptionEn())
                .email(center.getEmail())
                .phone(center.getPhone())
                .alternativePhone(center.getAlternativePhone())
                .address(center.getAddress())
                .latitude(center.getLatitude())
                .longitude(center.getLongitude())
                .openingTime(center.getOpeningTime())
                .closingTime(center.getClosingTime())
                .workingDays(center.getWorkingDays())
                .averageRating(center.getAverageRating())
                .totalReviews(center.getTotalReviews())
                .isVerified(center.getIsVerified())
                .isActive(center.getIsActive())
                .enabled(center.getEnabled())
                .ownerId(center.getOwner() != null ? center.getOwner().getId() : null)
                .ownerName(center.getOwner() != null ? center.getOwner().fullName() : null)
                .ownerEmail(center.getOwner() != null ? center.getOwner().getEmail() : null)
                .approvalStatus(center.getOwner() != null ? center.getOwner().getApprovalStatus() : null)
                .categories(categories)
                .specializations(center.getSpecializations())
                .logoUrl(center.getLogoUrl())
                .imageUrls(center.getImageUrls())
                .certifications(center.getCertifications())
                .createdAt(center.getCreatedAt())
                .build();
    }

    private AdminBookingResponse toAdminBookingResponse(Booking booking) {
        return AdminBookingResponse.builder()
                .id(booking.getId())
                .customerName(booking.getCustomer() != null ? booking.getCustomer().fullName() : null)
                .customerEmail(booking.getCustomer() != null ? booking.getCustomer().getEmail() : null)
                .centerNameAr(booking.getCenter() != null ? booking.getCenter().getNameAr() : null)
                .centerNameEn(booking.getCenter() != null ? booking.getCenter().getNameEn() : null)
                .serviceType(booking.getServiceType())
                .bookingStatus(booking.getBookingStatus())
                .bookingDate(booking.getBookingDate())
                .bookingTime(booking.getBookingTime())
                .createdAt(booking.getCreatedAt())
                .build();
    }

    private ComplaintResponse toAdminComplaintResponse(Complaint complaint) {
        return ComplaintResponse.builder()
                .id(complaint.getId())
                .complaintNumber(complaint.getComplaintNumber())
                .type(complaint.getType())
                .subject(complaint.getSubject())
                .description(complaint.getDescription())
                .status(complaint.getStatus())
                .priority(complaint.getPriority())
                .resolution(complaint.getResolution())
                .centerId(complaint.getCenter() != null ? complaint.getCenter().getId() : null)
                .centerNameAr(complaint.getCenter() != null ? complaint.getCenter().getNameAr() : null)
                .centerNameEn(complaint.getCenter() != null ? complaint.getCenter().getNameEn() : null)
                .bookingId(complaint.getBooking() != null ? complaint.getBooking().getId() : null)
                .createdAt(complaint.getCreatedAt())
                .resolvedAt(complaint.getResolvedAt())
                .build();
    }

    // Category management methods
    public List<ServiceCategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::toServiceCategoryResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ServiceCategoryResponse createCategory(ServiceCategoryRequest request) {
        if (categoryRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Category code already exists: " + request.getCode());
        }
        
        ServiceCategory category = ServiceCategory.builder()
                .code(request.getCode())
                .nameAr(request.getNameAr())
                .nameEn(request.getNameEn())
                .descriptionAr(request.getDescriptionAr())
                .descriptionEn(request.getDescriptionEn())
                .iconUrl(request.getIconUrl())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        
        category = categoryRepository.save(category);
        log.info("Admin created category with code: {}", request.getCode());
        return toServiceCategoryResponse(category);
    }

    @Transactional
    public ServiceCategoryResponse updateCategory(Long id, ServiceCategoryRequest request) {
        ServiceCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + id));
        
        // Check if code is being changed and if new code already exists
        if (!category.getCode().equals(request.getCode()) && categoryRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Category code already exists: " + request.getCode());
        }
        
        category.setCode(request.getCode());
        category.setNameAr(request.getNameAr());
        category.setNameEn(request.getNameEn());
        category.setDescriptionAr(request.getDescriptionAr());
        category.setDescriptionEn(request.getDescriptionEn());
        category.setIconUrl(request.getIconUrl());
        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }
        
        category = categoryRepository.save(category);
        log.info("Admin updated category ID: {}", id);
        return toServiceCategoryResponse(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        ServiceCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + id));
        
        if (categoryRepository.existsByCentersId(id)) {
            throw new IllegalStateException("Cannot delete category that is referenced by one or more centers");
        }
        
        categoryRepository.delete(category);
        log.info("Admin deleted category ID: {}", id);
    }

    private ServiceCategoryResponse toServiceCategoryResponse(ServiceCategory category) {
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
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .parentNameAr(category.getParent() != null ? category.getParent().getNameAr() : null)
                .parentNameEn(category.getParent() != null ? category.getParent().getNameEn() : null)
                .subcategoryCount(category.getSubcategories() != null ? category.getSubcategories().size() : 0)
                .centerCount(category.getCenters() != null ? category.getCenters().size() : 0)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
