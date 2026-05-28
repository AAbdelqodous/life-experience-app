package com.maintainance.service_center.department;

import com.maintainance.service_center.booking.BookingRepository;
import com.maintainance.service_center.booking.BookingStatus;
import com.maintainance.service_center.category.ServiceCategory;
import com.maintainance.service_center.category.ServiceCategoryRepository;
import com.maintainance.service_center.center.CenterResolverService;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.handler.BusinessErrorCodes;
import com.maintainance.service_center.staff.CenterMembership;
import com.maintainance.service_center.staff.CenterMembershipRepository;
import com.maintainance.service_center.staff.CenterMembershipResponse;
import com.maintainance.service_center.staff.CenterPermission;
import com.maintainance.service_center.staff.CenterRole;
import com.maintainance.service_center.staff.CenterSecurityService;
import com.maintainance.service_center.staff.MembershipStatus;
import com.maintainance.service_center.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentService {

    public static final String GENERAL_NAME_AR = "عام";
    public static final String GENERAL_NAME_EN = "General";

    private static final Set<BookingStatus> TERMINAL_STATUSES =
            Set.of(BookingStatus.COMPLETED, BookingStatus.CANCELLED, BookingStatus.NO_SHOW);

    private final DepartmentRepository departmentRepository;
    private final CenterMembershipRepository membershipRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final BookingRepository bookingRepository;
    private final CenterResolverService centerResolver;
    private final CenterSecurityService centerSecurity;

    public List<DepartmentResponse> getDepartments(User caller) {
        MaintenanceCenter center = centerResolver.resolveCenter(caller);
        centerSecurity.requireActiveMembership(center.getId(), caller);

        return departmentRepository.findByCenterIdActiveFirst(center.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DepartmentResponse createDepartment(CreateDepartmentRequest request, User caller) {
        MaintenanceCenter center = centerResolver.resolveCenter(caller);
        centerSecurity.requirePermission(center.getId(), caller, CenterPermission.MANAGE_NON_MANAGER_STAFF);

        ensureNameAvailable(center.getId(), request.getNameAr(), request.getNameEn(), null);
        List<ServiceCategory> categories = resolveCategories(request.getCategoryIds());

        int displayOrder = request.getDisplayOrder() != null
                ? request.getDisplayOrder()
                : nextDisplayOrder(center.getId());

        boolean isDiagnostic = Boolean.TRUE.equals(request.getIsDiagnostic());
        // Spec 022 invariants enforced before save.
        validateDiagnosticInvariants(center.getId(), null, isDiagnostic,
                request.getDiagnosticFeeAmount());

        Department department = Department.builder()
                .center(center)
                .nameAr(request.getNameAr())
                .nameEn(request.getNameEn())
                .displayOrder(displayOrder)
                .isActive(true)
                .categories(new ArrayList<>(categories))
                .isDiagnostic(isDiagnostic)
                .diagnosticFeeAmount(isDiagnostic ? request.getDiagnosticFeeAmount() : null)
                .build();

        department = departmentRepository.save(department);
        log.info("Created department id={} for center id={} isDiagnostic={}",
                department.getId(), center.getId(), isDiagnostic);
        return toResponse(department);
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long departmentId, UpdateDepartmentRequest request, User caller) {
        MaintenanceCenter center = centerResolver.resolveCenter(caller);
        centerSecurity.requirePermission(center.getId(), caller, CenterPermission.MANAGE_NON_MANAGER_STAFF);

        Department department = departmentRepository.findByIdAndCenterId(departmentId, center.getId())
                .orElseThrow(() -> new EntityNotFoundException("Department not found"));

        String newNameAr = request.getNameAr() != null ? request.getNameAr() : department.getNameAr();
        String newNameEn = request.getNameEn() != null ? request.getNameEn() : department.getNameEn();
        if (request.getNameAr() != null || request.getNameEn() != null) {
            ensureNameAvailable(center.getId(), newNameAr, newNameEn, departmentId);
        }

        if (request.getNameAr() != null) department.setNameAr(newNameAr);
        if (request.getNameEn() != null) department.setNameEn(newNameEn);
        if (request.getDisplayOrder() != null) department.setDisplayOrder(request.getDisplayOrder());
        if (request.getCategoryIds() != null) {
            department.setCategories(new ArrayList<>(resolveCategories(request.getCategoryIds())));
        }

        // Spec 022 — partial update for diagnostic fields. Both fields optional;
        // either one being non-null triggers the invariant check.
        boolean touchingDiagnostic = request.getIsDiagnostic() != null
                || request.getDiagnosticFeeAmount() != null;
        if (touchingDiagnostic) {
            boolean targetIsDiagnostic = request.getIsDiagnostic() != null
                    ? request.getIsDiagnostic()
                    : Boolean.TRUE.equals(department.getIsDiagnostic());
            java.math.BigDecimal targetFee = request.getDiagnosticFeeAmount() != null
                    ? request.getDiagnosticFeeAmount()
                    : department.getDiagnosticFeeAmount();

            // Block toggling the flag if non-terminal bookings exist (FR-DR-003).
            if (request.getIsDiagnostic() != null
                    && !request.getIsDiagnostic().equals(department.getIsDiagnostic())
                    && countNonTerminalBookings(departmentId) > 0) {
                throw new DepartmentOperationException(
                        BusinessErrorCodes.DIAGNOSTIC_TOGGLE_BLOCKED_BY_OPEN_BOOKINGS);
            }

            validateDiagnosticInvariants(center.getId(), departmentId, targetIsDiagnostic, targetFee);

            department.setIsDiagnostic(targetIsDiagnostic);
            // When flag flips off, clear the fee; otherwise persist the (possibly null) request value.
            department.setDiagnosticFeeAmount(targetIsDiagnostic ? targetFee : null);
        }

        department = departmentRepository.save(department);
        log.info("Updated department id={}", departmentId);
        return toResponse(department);
    }

    // Spec 022 invariants (FR-DR-001, FR-DR-002):
    //  - at most one active diagnostic dept per center (FR-DR-001)
    //  - diagnosticFeeAmount may only be set when isDiagnostic=true (FR-DR-002)
    // excludeDeptId is the dept being updated (or null on create) so it doesn't count itself.
    private void validateDiagnosticInvariants(Long centerId, Long excludeDeptId,
                                              boolean isDiagnostic, java.math.BigDecimal feeAmount) {
        if (!isDiagnostic && feeAmount != null) {
            throw new DepartmentOperationException(
                    BusinessErrorCodes.INVALID_DIAGNOSTIC_FEE_TARGET);
        }
        if (isDiagnostic) {
            departmentRepository.findDiagnosticByCenterId(centerId).ifPresent(existing -> {
                if (excludeDeptId == null || !existing.getId().equals(excludeDeptId)) {
                    throw new DepartmentOperationException(
                            BusinessErrorCodes.DUPLICATE_DIAGNOSTIC_DEPARTMENT);
                }
            });
        }
    }

    @Transactional
    public void deactivateDepartment(Long departmentId, User caller) {
        MaintenanceCenter center = centerResolver.resolveCenter(caller);
        centerSecurity.requirePermission(center.getId(), caller, CenterPermission.MANAGE_NON_MANAGER_STAFF);

        Department department = departmentRepository.findByIdAndCenterId(departmentId, center.getId())
                .orElseThrow(() -> new EntityNotFoundException("Department not found"));

        if (!Boolean.TRUE.equals(department.getIsActive())) {
            return; // already inactive — idempotent
        }

        long openBookings = countNonTerminalBookings(departmentId);
        if (openBookings > 0) {
            throw new DepartmentOperationException(BusinessErrorCodes.DEPT_HAS_OPEN_BOOKINGS);
        }

        long activeMembers = membershipRepository.countByDepartmentIdAndStatus(
                departmentId, MembershipStatus.ACTIVE);
        if (activeMembers > 0) {
            throw new DepartmentOperationException(BusinessErrorCodes.DEPT_HAS_ACTIVE_MEMBERS);
        }

        long activeDepts = departmentRepository.countActiveByCenterId(center.getId());
        if (activeDepts <= 1) {
            throw new DepartmentOperationException(BusinessErrorCodes.DEPT_LAST_ACTIVE);
        }

        department.setIsActive(false);
        departmentRepository.save(department);
        log.info("Deactivated department id={}", departmentId);
    }

    public List<CenterMembershipResponse> getDepartmentMembers(Long departmentId, User caller) {
        MaintenanceCenter center = centerResolver.resolveCenter(caller);
        centerSecurity.requireActiveMembership(center.getId(), caller);

        Department department = departmentRepository.findByIdAndCenterId(departmentId, center.getId())
                .orElseThrow(() -> new EntityNotFoundException("Department not found"));

        return membershipRepository.findByDepartmentIdAndStatus(department.getId(), MembershipStatus.ACTIVE)
                .stream()
                .map(this::toMembershipResponse)
                .toList();
    }

    @Transactional
    public DepartmentResponse addDepartmentMember(Long departmentId, Long membershipId, User caller) {
        MaintenanceCenter center = centerResolver.resolveCenter(caller);
        centerSecurity.requirePermission(center.getId(), caller, CenterPermission.MANAGE_NON_MANAGER_STAFF);

        Department department = departmentRepository.findByIdAndCenterId(departmentId, center.getId())
                .orElseThrow(() -> new EntityNotFoundException("Department not found"));

        CenterMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new EntityNotFoundException("Membership not found"));

        if (!membership.getCenter().getId().equals(center.getId())) {
            throw new DepartmentOperationException(BusinessErrorCodes.DEPT_MEMBER_WRONG_CENTER);
        }
        if (membership.getRole() != CenterRole.TECHNICIAN) {
            throw new DepartmentOperationException(BusinessErrorCodes.DEPT_MEMBER_NOT_TECHNICIAN);
        }

        boolean alreadyAssigned = membership.getDepartments().stream()
                .anyMatch(d -> d.getId().equals(departmentId));
        if (alreadyAssigned) {
            throw new DepartmentOperationException(BusinessErrorCodes.DEPT_MEMBER_ALREADY_ASSIGNED);
        }

        membership.getDepartments().add(department);
        membershipRepository.save(membership);
        log.info("Added membership id={} to department id={}", membershipId, departmentId);

        return toResponse(department);
    }

    @Transactional
    public void removeDepartmentMember(Long departmentId, Long membershipId, User caller) {
        MaintenanceCenter center = centerResolver.resolveCenter(caller);
        centerSecurity.requirePermission(center.getId(), caller, CenterPermission.MANAGE_NON_MANAGER_STAFF);

        Department department = departmentRepository.findByIdAndCenterId(departmentId, center.getId())
                .orElseThrow(() -> new EntityNotFoundException("Department not found"));

        CenterMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new EntityNotFoundException("Membership not found"));

        if (!membership.getCenter().getId().equals(center.getId())) {
            throw new EntityNotFoundException("Membership not found at this center");
        }

        boolean removed = membership.getDepartments().removeIf(d -> d.getId().equals(department.getId()));
        if (!removed) {
            throw new EntityNotFoundException("Membership is not assigned to this department");
        }
        membershipRepository.save(membership);
        log.info("Removed membership id={} from department id={}", membershipId, departmentId);
    }

    /**
     * Resolve the department a new booking should be routed to based on its category.
     * <p>Lookup order (FR-D-007 to FR-D-009):
     * <ol>
     *   <li>Active departments at the center that cover the given category, lowest displayOrder wins.</li>
     *   <li>Else, the center's first active department (General by convention) as fallback.</li>
     *   <li>Else, ensure a General department exists, create it, and return it.</li>
     * </ol>
     */
    @Transactional
    public Department resolveForCategory(MaintenanceCenter center, ServiceCategory category) {
        if (center == null) {
            return null;
        }
        if (category != null) {
            List<Department> matches = departmentRepository.findActiveByCenterAndCategory(center, category);
            if (!matches.isEmpty()) {
                return matches.get(0);
            }
        }
        List<Department> active = departmentRepository.findActiveByCenter(center);
        if (!active.isEmpty()) {
            return active.get(0);
        }
        // Safety net: no active dept at all — seed General now (FR-D-006 says this shouldn't happen,
        // but for legacy centers that pre-date seeding, this keeps booking creation safe).
        return ensureGeneralDepartment(center);
    }

    /**
     * Idempotent: returns the existing active General department for the center, or creates one.
     * Used by the seeding routine and by the safety net in {@link #resolveForCategory}.
     */
    @Transactional
    public Department ensureGeneralDepartment(MaintenanceCenter center) {
        return departmentRepository.findActiveByCenterIdAndNameEn(center.getId(), GENERAL_NAME_EN)
                .or(() -> departmentRepository.findActiveByCenterIdAndNameAr(center.getId(), GENERAL_NAME_AR))
                .orElseGet(() -> {
                    Department general = Department.builder()
                            .center(center)
                            .nameAr(GENERAL_NAME_AR)
                            .nameEn(GENERAL_NAME_EN)
                            .displayOrder(0)
                            .isActive(true)
                            .categories(new ArrayList<>())
                            .build();
                    Department saved = departmentRepository.save(general);
                    log.info("Seeded General department id={} for center id={}", saved.getId(), center.getId());
                    return saved;
                });
    }

    private void ensureNameAvailable(Long centerId, String nameAr, String nameEn, Long excludeDeptId) {
        departmentRepository.findActiveByCenterIdAndNameAr(centerId, nameAr).ifPresent(d -> {
            if (excludeDeptId == null || !d.getId().equals(excludeDeptId)) {
                throw new DepartmentOperationException(BusinessErrorCodes.DEPT_DUPLICATE_NAME_AR);
            }
        });
        departmentRepository.findActiveByCenterIdAndNameEn(centerId, nameEn).ifPresent(d -> {
            if (excludeDeptId == null || !d.getId().equals(excludeDeptId)) {
                throw new DepartmentOperationException(BusinessErrorCodes.DEPT_DUPLICATE_NAME_EN);
            }
        });
    }

    private List<ServiceCategory> resolveCategories(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<ServiceCategory> found = categoryRepository.findAllById(categoryIds);
        if (found.size() != categoryIds.size()) {
            throw new DepartmentOperationException(BusinessErrorCodes.DEPT_INVALID_CATEGORY);
        }
        return found;
    }

    private int nextDisplayOrder(Long centerId) {
        return departmentRepository.findByCenterIdAndIsActiveTrueOrderByDisplayOrderAscIdAsc(centerId)
                .stream()
                .map(Department::getDisplayOrder)
                .max(Comparator.naturalOrder())
                .map(max -> max + 1)
                .orElse(0);
    }

    private long countNonTerminalBookings(Long departmentId) {
        // Non-terminal = not in {COMPLETED, CANCELLED, NO_SHOW}
        return bookingRepository.countNonTerminalByDepartmentId(departmentId, TERMINAL_STATUSES);
    }

    private DepartmentResponse toResponse(Department department) {
        return DepartmentResponse.builder()
                .id(department.getId())
                .centerId(department.getCenter().getId())
                .nameAr(department.getNameAr())
                .nameEn(department.getNameEn())
                .displayOrder(department.getDisplayOrder())
                .isActive(department.getIsActive())
                .categoryIds(department.getCategoryIds())
                .memberCount((int) membershipRepository.countByDepartmentIdAndStatus(
                        department.getId(), MembershipStatus.ACTIVE))
                .isDiagnostic(Boolean.TRUE.equals(department.getIsDiagnostic()))
                .diagnosticFeeAmount(department.getDiagnosticFeeAmount())
                .build();
    }

    private CenterMembershipResponse toMembershipResponse(CenterMembership membership) {
        return CenterMembershipResponse.builder()
                .id(membership.getId())
                .userId(membership.getUser().getId())
                .userFirstname(membership.getUser().getFirstname())
                .userLastname(membership.getUser().getLastname())
                .userEmail(membership.getUser().getEmail())
                .role(membership.getRole())
                .roleAr(membership.getRole().getArabic())
                .roleEn(membership.getRole().getEnglish())
                .status(membership.getStatus())
                .invitedByName(membership.getInvitedBy() != null ? membership.getInvitedBy().fullName() : null)
                .activatedAt(membership.getActivatedAt())
                .centerId(membership.getCenter().getId())
                .build();
    }
}
