package com.maintainance.service_center.department;

import com.maintainance.service_center.category.ServiceCategory;
import com.maintainance.service_center.category.ServiceCategoryRepository;
import com.maintainance.service_center.center.CenterResolverService;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.staff.*;
import com.maintainance.service_center.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final CenterMembershipRepository membershipRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final CenterResolverService centerResolver;
    private final CenterSecurityService centerSecurity;

    public List<DepartmentResponse> getDepartments(User caller) {
        MaintenanceCenter center = centerResolver.resolveCenter(caller);
        centerSecurity.requireActiveMembership(center.getId(), caller);

        return departmentRepository.findByCenterIdOrderByDisplayOrderAsc(center.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DepartmentResponse createDepartment(CreateDepartmentRequest request, User caller) {
        MaintenanceCenter center = centerResolver.resolveCenter(caller);
        centerSecurity.requirePermission(center.getId(), caller, CenterPermission.EDIT_CENTER_PROFILE);

        Department department = Department.builder()
                .center(center)
                .nameAr(request.getNameAr())
                .nameEn(request.getNameEn())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .categories(resolveCategories(request.getCategoryIds()))
                .build();

        department = departmentRepository.save(department);
        log.info("Created department id={} for center id={}", department.getId(), center.getId());
        return toResponse(department);
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long departmentId, UpdateDepartmentRequest request, User caller) {
        MaintenanceCenter center = centerResolver.resolveCenter(caller);
        centerSecurity.requirePermission(center.getId(), caller, CenterPermission.EDIT_CENTER_PROFILE);

        Department department = departmentRepository.findByIdAndCenterId(departmentId, center.getId())
                .orElseThrow(() -> new EntityNotFoundException("Department not found"));

        if (request.getNameAr() != null) {
            department.setNameAr(request.getNameAr());
        }
        if (request.getNameEn() != null) {
            department.setNameEn(request.getNameEn());
        }
        if (request.getDisplayOrder() != null) {
            department.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getCategoryIds() != null) {
            department.setCategories(resolveCategories(request.getCategoryIds()));
        }

        department = departmentRepository.save(department);
        log.info("Updated department id={}", departmentId);
        return toResponse(department);
    }

    @Transactional
    public void deactivateDepartment(Long departmentId, User caller) {
        MaintenanceCenter center = centerResolver.resolveCenter(caller);
        centerSecurity.requirePermission(center.getId(), caller, CenterPermission.EDIT_CENTER_PROFILE);

        Department department = departmentRepository.findByIdAndCenterId(departmentId, center.getId())
                .orElseThrow(() -> new EntityNotFoundException("Department not found"));

        department.setIsActive(false);
        departmentRepository.save(department);
        log.info("Deactivated department id={}", departmentId);
    }

    public List<CenterMembershipResponse> getDepartmentMembers(Long departmentId, User caller) {
        MaintenanceCenter center = centerResolver.resolveCenter(caller);
        centerSecurity.requireActiveMembership(center.getId(), caller);

        Department department = departmentRepository.findByIdAndCenterId(departmentId, center.getId())
                .orElseThrow(() -> new EntityNotFoundException("Department not found"));

        return membershipRepository.findByCenterIdAndStatus(center.getId(), MembershipStatus.ACTIVE,
                        org.springframework.data.domain.Pageable.unpaged())
                .stream()
                .filter(m -> m.getDepartmentIds().contains(departmentId))
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
            throw new AccessDeniedException("Membership does not belong to this center");
        }

        if (!membership.getDepartmentIds().contains(departmentId)) {
            membership.getDepartments().add(department);
            membershipRepository.save(membership);
            log.info("Added membership id={} to department id={}", membershipId, departmentId);
        }

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
            throw new AccessDeniedException("Membership does not belong to this center");
        }

        membership.getDepartments().removeIf(d -> d.getId().equals(departmentId));
        membershipRepository.save(membership);
        log.info("Removed membership id={} from department id={}", membershipId, departmentId);
    }

    private List<ServiceCategory> resolveCategories(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return new ArrayList<>();
        }
        return categoryRepository.findAllById(categoryIds);
    }

    private int countMembers(Department department) {
        return (int) membershipRepository.findByCenterIdAndStatus(
                        department.getCenter().getId(), MembershipStatus.ACTIVE,
                        org.springframework.data.domain.Pageable.unpaged())
                .stream()
                .filter(m -> m.getDepartmentIds().contains(department.getId()))
                .count();
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
                .memberCount(countMembers(department))
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
