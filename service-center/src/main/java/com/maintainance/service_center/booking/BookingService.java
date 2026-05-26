package com.maintainance.service_center.booking;

import com.maintainance.service_center.category.ServiceCategory;
import com.maintainance.service_center.center.CenterResolverService;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.department.Department;
import com.maintainance.service_center.department.DepartmentService;
import com.maintainance.service_center.service.CenterService;
import com.maintainance.service_center.service.CenterServiceRepository;
import com.maintainance.service_center.staff.CenterMembership;
import com.maintainance.service_center.staff.CenterMembershipRepository;
import com.maintainance.service_center.staff.CenterPermission;
import com.maintainance.service_center.staff.CenterRole;
import com.maintainance.service_center.staff.CenterSecurityService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final MaintenanceCenterRepository centerRepository;
    private final CenterResolverService centerResolver;
    private final CenterServiceRepository centerServiceRepository;
    private final CenterMembershipRepository membershipRepository;
    private final BookingClaimAuditRepository claimAuditRepository;
    private final CenterSecurityService centerSecurity;
    private final BookingStatusHistoryRepository statusHistoryRepository;
    private final DepartmentService departmentService;

    private static final Set<BookingStatus> CLAIMABLE_STATUSES = Set.of(
            BookingStatus.CONFIRMED, BookingStatus.RESCHEDULED);

    public Page<BookingResponse> findByCustomer(User customer, Pageable pageable) {
        return bookingRepository.findByCustomer_IdOrderByCreatedAtDesc(customer.getId(), pageable)
                .map(this::toResponse);
    }

    public Page<BookingResponse> findByCenter(Long centerId, BookingStatus status, User caller, Pageable pageable) {
        centerRepository.findById(centerId)
                .orElseThrow(() -> new EntityNotFoundException("Center not found with id: " + centerId));
        centerResolver.assertAccess(centerId, caller);
        if (status != null) {
            return bookingRepository.findByCenterIdAndStatusesPageable(centerId, List.of(status), pageable)
                    .map(this::toResponse);
        }
        return bookingRepository.findByCenter_IdOrderByCreatedAtDesc(centerId, pageable)
                .map(this::toResponse);
    }

    public BookingResponse findByBookingNumber(String bookingNumber) {
        return toResponse(getBookingByNumber(bookingNumber));
    }

    public BookingResponse findById(Long id, User caller) {
        Booking booking = getBooking(id);
        checkOwnershipOrAccess(booking, caller);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse create(BookingRequest request, User customer) {
        MaintenanceCenter center = centerRepository.findById(request.getCenterId())
                .orElseThrow(() -> new EntityNotFoundException("Center not found with id: " + request.getCenterId()));

        if (!center.getIsActive()) {
            throw new IllegalArgumentException("Cannot create booking for inactive center");
        }

        boolean hasNewFields = request.getCategoryId() != null && request.getServiceId() != null;
        boolean hasLegacyField = request.getServiceType() != null;

        if (!hasNewFields && !hasLegacyField) {
            throw new IllegalArgumentException(
                    "Either (categoryId + serviceId) or serviceType is required to create a booking");
        }

        com.maintainance.service_center.service.Service service = null;
        ServiceCategory category = null;
        ServiceType serviceType = request.getServiceType();

        if (hasNewFields) {
            CenterService offering = centerServiceRepository
                    .findByCenterIdAndCategoryIdAndServiceIdAndIsActiveTrue(
                            center.getId(), request.getCategoryId(), request.getServiceId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "This center does not actively offer the requested service for the given category"));
            service = offering.getService();
            category = offering.getCategory();
            if (hasLegacyField) {
                log.info("Booking created with both new (categoryId/serviceId) and legacy (serviceType) fields; " +
                         "new fields take precedence for center id={}", center.getId());
            }
        } else {
            log.warn("Booking created via deprecated serviceType field for center id={}; " +
                     "please migrate clients to use categoryId + serviceId", center.getId());
        }

        String bookingNumber = generateBookingNumber();

        // Spec 020 — route booking to a department based on its category.
        // Falls back to the center's General department when no active dept covers the category.
        Department department = departmentService.resolveForCategory(center, category);

        Booking booking = Booking.builder()
                .bookingNumber(bookingNumber)
                .customer(customer)
                .center(center)
                .bookingDate(request.getBookingDate())
                .bookingTime(request.getBookingTime())
                .estimatedEndTime(request.getEstimatedEndTime())
                .bookingStatus(BookingStatus.PENDING)
                .serviceType(serviceType)
                .service(service)
                .category(category)
                .department(department)
                .serviceDescription(request.getServiceDescription())
                .problemDescription(request.getProblemDescription())
                .requestedServices(request.getRequestedServices() != null ? request.getRequestedServices() : List.of())
                .deviceType(request.getDeviceType())
                .deviceModel(request.getDeviceModel())
                .deviceYear(request.getDeviceYear())
                .deviceSerial(request.getDeviceSerial())
                .problemImageUrls(request.getProblemImageUrls() != null ? request.getProblemImageUrls() : List.of())
                .estimatedCost(request.getEstimatedCost())
                .customerPhone(request.getCustomerPhone())
                .customerAlternativePhone(request.getCustomerAlternativePhone())
                .customerAddress(request.getCustomerAddress())
                .specialInstructions(request.getSpecialInstructions())
                .isUrgent(request.getIsUrgent() != null ? request.getIsUrgent() : false)
                .pickupRequired(request.getPickupRequired() != null ? request.getPickupRequired() : false)
                .pickupAddress(request.getPickupAddress())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        bookingRepository.save(booking);
        log.info("Created booking number={} for customer id={} at center id={}",
                bookingNumber, customer.getId(), center.getId());
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse update(Long id, BookingRequest request, User caller) {
        Booking booking = getBooking(id);
        checkOwnershipOrAccess(booking, caller);

        if (booking.getBookingStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Cannot update booking that is not pending");
        }

        if (request.getCenterId() != null && !booking.getCenter().getId().equals(request.getCenterId())) {
            MaintenanceCenter center = centerRepository.findById(request.getCenterId())
                    .orElseThrow(() -> new EntityNotFoundException("Center not found with id: " + request.getCenterId()));
            if (!center.getIsActive()) {
                throw new IllegalArgumentException("Cannot update booking to inactive center");
            }
            booking.setCenter(center);
        }

        booking.setBookingDate(request.getBookingDate());
        booking.setBookingTime(request.getBookingTime());
        booking.setEstimatedEndTime(request.getEstimatedEndTime());
        booking.setServiceType(request.getServiceType());
        booking.setServiceDescription(request.getServiceDescription());
        booking.setProblemDescription(request.getProblemDescription());
        booking.setRequestedServices(request.getRequestedServices() != null ? request.getRequestedServices() : List.of());
        booking.setDeviceType(request.getDeviceType());
        booking.setDeviceModel(request.getDeviceModel());
        booking.setDeviceYear(request.getDeviceYear());
        booking.setDeviceSerial(request.getDeviceSerial());
        booking.setProblemImageUrls(request.getProblemImageUrls() != null ? request.getProblemImageUrls() : List.of());
        booking.setEstimatedCost(request.getEstimatedCost());
        booking.setCustomerPhone(request.getCustomerPhone());
        booking.setCustomerAlternativePhone(request.getCustomerAlternativePhone());
        booking.setCustomerAddress(request.getCustomerAddress());
        booking.setSpecialInstructions(request.getSpecialInstructions());
        booking.setIsUrgent(request.getIsUrgent() != null ? request.getIsUrgent() : booking.getIsUrgent());
        booking.setPickupRequired(request.getPickupRequired() != null ? request.getPickupRequired() : booking.getPickupRequired());
        booking.setPickupAddress(request.getPickupAddress());
        booking.setPaymentMethod(request.getPaymentMethod());

        bookingRepository.save(booking);
        log.info("Updated booking id={}", id);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse confirm(Long id, User caller) {
        Booking booking = getBooking(id);
        CenterMembership membership = checkCenterAccess(booking, caller);

        if (booking.getBookingStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Can only confirm pending bookings");
        }

        BookingStatus oldStatus = booking.getBookingStatus();
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        recordStatusChange(booking, oldStatus, BookingStatus.CONFIRMED, caller, membership.getRole(), null);
        log.info("Confirmed booking id={}", id);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse startService(Long id, User caller) {
        Booking booking = getBooking(id);
        CenterMembership membership = checkCenterAccess(booking, caller);

        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException("Can only start confirmed bookings");
        }

        BookingStatus oldStatus = booking.getBookingStatus();
        booking.setBookingStatus(BookingStatus.IN_PROGRESS);
        bookingRepository.save(booking);
        recordStatusChange(booking, oldStatus, BookingStatus.IN_PROGRESS, caller, membership.getRole(), null);
        log.info("Started service for booking id={}", id);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse complete(Long id, BookingCompletionRequest request, User caller) {
        Booking booking = getBooking(id);
        CenterMembership membership = checkCenterAccess(booking, caller);

        if (booking.getBookingStatus() != BookingStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Can only complete in-progress bookings");
        }

        PaymentStatus resolvedPaymentStatus = request.getPaymentStatus() != null
                ? request.getPaymentStatus()
                : PaymentStatus.PAID;

        BookingStatus oldStatus = booking.getBookingStatus();
        booking.setBookingStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(LocalDateTime.now());
        booking.setCompletionNotes(request.getCompletionNotes());
        booking.setFinalCost(request.getFinalCost());
        booking.setCostNotes(request.getCostNotes());
        booking.setCompletionImageUrls(request.getCompletionImageUrls() != null ? request.getCompletionImageUrls() : List.of());
        booking.setPaymentStatus(resolvedPaymentStatus);
        if (resolvedPaymentStatus == PaymentStatus.PAID) {
            booking.setPaidAt(LocalDateTime.now());
        }

        bookingRepository.save(booking);
        recordStatusChange(booking, oldStatus, BookingStatus.COMPLETED, caller, membership.getRole(),
                request.getCompletionNotes());
        log.info("Completed booking id={}", id);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse cancel(Long id, BookingCancellationRequest request, User caller) {
        Booking booking = getBooking(id);
        checkOwnershipOrAccess(booking, caller);

        if (booking.getBookingStatus() == BookingStatus.COMPLETED ||
            booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot cancel completed or cancelled booking");
        }

        boolean isCustomer = caller.getId().equals(booking.getCustomer().getId());
        BookingStatus oldStatus = booking.getBookingStatus();
        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancelledReason(request.getReason());
        booking.setCancelledBy(isCustomer ? CancelledBy.CUSTOMER : CancelledBy.CENTER);

        bookingRepository.save(booking);

        // Record audit — resolve role for center staff; customers get null role
        CenterRole actingRole = null;
        if (!isCustomer) {
            actingRole = centerSecurity.resolveMembership(booking.getCenter().getId(), caller)
                    .map(m -> m.getRole())
                    .orElse(null);
        }
        if (actingRole != null) {
            recordStatusChange(booking, oldStatus, BookingStatus.CANCELLED, caller, actingRole,
                    request.getReason());
        }
        log.info("Cancelled booking id={} by {}", id, booking.getCancelledBy());
        return toResponse(booking);
    }

    public BookingStatsResponse getMyStats(User customer) {
        Integer customerId = customer.getId();
        return BookingStatsResponse.builder()
                .total(bookingRepository.countByCustomerId(customerId))
                .pending(bookingRepository.countByCustomerIdAndStatus(customerId, BookingStatus.PENDING))
                .confirmed(bookingRepository.countByCustomerIdAndStatus(customerId, BookingStatus.CONFIRMED))
                .inProgress(bookingRepository.countByCustomerIdAndStatus(customerId, BookingStatus.IN_PROGRESS))
                .completed(bookingRepository.countByCustomerIdAndStatus(customerId, BookingStatus.COMPLETED))
                .cancelled(bookingRepository.countByCustomerIdAndStatus(customerId, BookingStatus.CANCELLED))
                .noShow(bookingRepository.countByCustomerIdAndStatus(customerId, BookingStatus.NO_SHOW))
                .rescheduled(bookingRepository.countByCustomerIdAndStatus(customerId, BookingStatus.RESCHEDULED))
                .totalRevenue(nullSafe(bookingRepository.sumFinalCostByCustomerId(customerId)))
                .build();
    }

    public BookingStatsResponse getCenterStats(User caller) {
        Long centerId = centerResolver.resolveCenter(caller).getId();
        return BookingStatsResponse.builder()
                .total(bookingRepository.countByCenterId(centerId))
                .pending(bookingRepository.countByCenterIdAndStatus(centerId, BookingStatus.PENDING))
                .confirmed(bookingRepository.countByCenterIdAndStatus(centerId, BookingStatus.CONFIRMED))
                .inProgress(bookingRepository.countByCenterIdAndStatus(centerId, BookingStatus.IN_PROGRESS))
                .completed(bookingRepository.countByCenterIdAndStatus(centerId, BookingStatus.COMPLETED))
                .cancelled(bookingRepository.countByCenterIdAndStatus(centerId, BookingStatus.CANCELLED))
                .noShow(bookingRepository.countByCenterIdAndStatus(centerId, BookingStatus.NO_SHOW))
                .rescheduled(bookingRepository.countByCenterIdAndStatus(centerId, BookingStatus.RESCHEDULED))
                .totalRevenue(nullSafe(bookingRepository.sumFinalCostByCenterId(centerId)))
                .build();
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    public List<BookingResponse> getMyBookings(User customer, BookingStatus status) {
        return bookingRepository.findByCustomerIdAndStatuses(customer.getId(), List.of(status))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Booking getBooking(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found with id: " + id));
    }

    private Booking getBookingByNumber(String bookingNumber) {
        return bookingRepository.findByBookingNumber(bookingNumber)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found with number: " + bookingNumber));
    }

    private void checkOwnershipOrAccess(Booking booking, User caller) {
        boolean isCustomer = caller.getId().equals(booking.getCustomer().getId());
        if (isCustomer) return;
        try {
            centerResolver.assertAccess(booking.getCenter().getId(), caller);
        } catch (AccessDeniedException e) {
            throw new IllegalArgumentException("You do not have permission to access this booking");
        }
    }

    private CenterMembership checkCenterAccess(Booking booking, User caller) {
        return centerSecurity.requirePermission(
                booking.getCenter().getId(), caller, CenterPermission.MANAGE_BOOKINGS);
    }

    private void recordStatusChange(Booking booking, BookingStatus oldStatus, BookingStatus newStatus,
                                     User actingUser, CenterRole actingRole, String notes) {
        statusHistoryRepository.save(BookingStatusHistory.builder()
                .booking(booking)
                .actingUser(actingUser)
                .actingRole(actingRole)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .notes(notes)
                .build());
    }

    private String generateBookingNumber() {
        String number;
        do {
            number = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (bookingRepository.existsByBookingNumber(number));
        return number;
    }

    private BookingResponse toResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .bookingNumber(booking.getBookingNumber())
                .customerId(booking.getCustomer().getId())
                .customerName(booking.getCustomer().fullName())
                .customerEmail(booking.getCustomer().getEmail())
                .customerPhone(booking.getCustomerPhone())
                .centerId(booking.getCenter().getId())
                .centerName(booking.getCenter().getNameEn())
                .centerPhone(booking.getCenter().getPhone())
                .centerAddress(booking.getCenter().getAddress())
                .centerLatitude(booking.getCenter().getLatitude())
                .centerLongitude(booking.getCenter().getLongitude())
                .bookingDate(booking.getBookingDate())
                .bookingTime(booking.getBookingTime())
                .estimatedEndTime(booking.getEstimatedEndTime())
                .bookingStatus(booking.getBookingStatus())
                .serviceType(booking.getServiceType())
                .serviceDescription(booking.getServiceDescription())
                .problemDescription(booking.getProblemDescription())
                .requestedServices(booking.getRequestedServices())
                .deviceType(booking.getDeviceType())
                .deviceModel(booking.getDeviceModel())
                .deviceYear(booking.getDeviceYear())
                .deviceSerial(booking.getDeviceSerial())
                .problemImageUrls(booking.getProblemImageUrls())
                .estimatedCost(booking.getEstimatedCost())
                .finalCost(booking.getFinalCost())
                .costNotes(booking.getCostNotes())
                .paymentMethod(booking.getPaymentMethod())
                .paymentStatus(booking.getPaymentStatus())
                .paidAt(booking.getPaidAt())
                .completedAt(booking.getCompletedAt())
                .completionNotes(booking.getCompletionNotes())
                .completionImageUrls(booking.getCompletionImageUrls())
                .cancelledAt(booking.getCancelledAt())
                .cancelledReason(booking.getCancelledReason())
                .cancelledBy(booking.getCancelledBy())
                .customerAddress(booking.getCustomerAddress())
                .specialInstructions(booking.getSpecialInstructions())
                .isUrgent(booking.getIsUrgent())
                .pickupRequired(booking.getPickupRequired())
                .pickupAddress(booking.getPickupAddress())
                .currentWorkStage(booking.getWorkStage())
                .serviceSummary(booking.getService() != null
                        ? BookingResponse.ServiceSummary.builder()
                                .id(booking.getService().getId())
                                .code(booking.getService().getCode())
                                .nameAr(booking.getService().getNameAr())
                                .nameEn(booking.getService().getNameEn())
                                .build()
                        : null)
                .categorySummary(booking.getCategory() != null
                        ? BookingResponse.CategorySummary.builder()
                                .id(booking.getCategory().getId())
                                .code(booking.getCategory().getCode())
                                .nameAr(booking.getCategory().getNameAr())
                                .nameEn(booking.getCategory().getNameEn())
                                .build()
                        : null)
                .assignedMembershipId(booking.getAssignedMembership() != null
                        ? booking.getAssignedMembership().getId() : null)
                .assignedStaffName(booking.getAssignedMembership() != null
                        ? booking.getAssignedMembership().getUser().fullName() : null)
                .departmentId(booking.getDepartment() != null
                        ? booking.getDepartment().getId() : null)
                .departmentNameAr(booking.getDepartment() != null
                        ? booking.getDepartment().getNameAr() : null)
                .departmentNameEn(booking.getDepartment() != null
                        ? booking.getDepartment().getNameEn() : null)
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }

    public BookingQueueResponse getQueue(User caller, Pageable pageable) {
        CenterMembership membership = resolveActiveMembership(caller);

        if (!membership.getRole().hasPermission(CenterPermission.CLAIM_BOOKING)) {
            throw new AccessDeniedException("CLAIM_BOOKING permission required");
        }

        List<Long> departmentIds = membership.getDepartmentIds();
        if (departmentIds.isEmpty()) {
            return BookingQueueResponse.builder()
                    .content(List.of())
                    .totalElements(0)
                    .totalPages(0)
                    .number(0)
                    .size(pageable.getPageSize())
                    .first(true)
                    .last(true)
                    .noDepartmentMembership(true)
                    .build();
        }

        Page<Booking> page = bookingRepository.findClaimableByDepartments(
                membership.getCenter(), departmentIds, pageable);

        return BookingQueueResponse.builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .number(page.getNumber())
                .size(page.getSize())
                .first(page.isFirst())
                .last(page.isLast())
                .noDepartmentMembership(false)
                .build();
    }

    @Transactional
    public BookingResponse claim(Long bookingId, User caller) {
        CenterMembership membership = resolveActiveMembership(caller);

        if (!membership.getRole().hasPermission(CenterPermission.CLAIM_BOOKING)) {
            throw new AccessDeniedException("CLAIM_BOOKING permission required");
        }

        // Precondition 1 — membership must be active
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new IllegalStateException("STAFF_INACTIVE");
        }

        // Preconditions 2+3 — outside lock (optimistic check)
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("BOOKING_NOT_CLAIMABLE"));
        if (!CLAIMABLE_STATUSES.contains(booking.getBookingStatus())) {
            throw new IllegalStateException("BOOKING_NOT_CLAIMABLE");
        }

        // Acquire pessimistic write lock for concurrent-safe checks
        Booking locked = bookingRepository.findWithLockById(bookingId)
                .orElseThrow(() -> new IllegalStateException("BOOKING_NOT_CLAIMABLE"));

        // Precondition 4 — already claimed (under lock)
        if (locked.getAssignedMembership() != null) {
            throw new IllegalStateException("BOOKING_ALREADY_CLAIMED");
        }

        // Precondition 5 — department match (under lock)
        if (locked.getDepartment() == null
                || !membership.getDepartmentIds().contains(locked.getDepartment().getId())) {
            throw new IllegalStateException("WRONG_DEPARTMENT");
        }

        locked.setAssignedMembership(membership);
        claimAuditRepository.save(BookingClaimAudit.builder()
                .booking(locked)
                .membershipId(membership.getId())
                .userId(caller.getId().longValue())
                .departmentId(locked.getDepartment().getId())
                .build());

        return toResponse(locked);
    }

    @Transactional
    public BookingResponse assign(Long bookingId, Long membershipId, User caller) {
        Booking booking = getBooking(bookingId);

        // Caller must have ASSIGN_TECHNICIAN_MANUAL permission at this center
        centerSecurity.requirePermission(
                booking.getCenter().getId(), caller, CenterPermission.ASSIGN_TECHNICIAN_MANUAL);

        if (membershipId == null) {
            // Unassign
            booking.setAssignedMembership(null);
            bookingRepository.save(booking);
            log.info("Unassigned booking id={}", bookingId);
            return toResponse(booking);
        }

        CenterMembership targetMembership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new EntityNotFoundException("Membership not found"));

        // Must be at the same center
        if (!targetMembership.getCenter().getId().equals(booking.getCenter().getId())) {
            throw new IllegalArgumentException("This technician does not belong to this branch");
        }

        // Must be active
        if (targetMembership.getStatus() != MembershipStatus.ACTIVE) {
            throw new IllegalArgumentException("Target staff member is not active");
        }

        // Must be a technician
        if (targetMembership.getRole() != CenterRole.TECHNICIAN) {
            throw new IllegalArgumentException("Only technicians can be assigned to bookings");
        }

        booking.setAssignedMembership(targetMembership);
        bookingRepository.save(booking);
        log.info("Assigned booking id={} to membership id={}", bookingId, membershipId);
        return toResponse(booking);
    }

    public Page<BookingResponse> findAssignedBookings(User caller, BookingStatus status, Pageable pageable) {
        CenterMembership membership = resolveActiveMembership(caller);
        Page<Booking> page;
        if (status != null) {
            page = bookingRepository.findByAssignedMembershipIdAndBookingStatus(
                    membership.getId(), status, pageable);
        } else {
            page = bookingRepository.findByAssignedMembershipId(membership.getId(), pageable);
        }
        return page.map(this::toResponse);
    }

    private CenterMembership resolveActiveMembership(User caller) {
        return membershipRepository.findByUserIdAndStatus(caller.getId(), MembershipStatus.ACTIVE)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("STAFF_INACTIVE"));
    }
}
