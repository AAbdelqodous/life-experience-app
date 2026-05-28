package com.maintainance.service_center.staff;

import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Central permission-enforcement service for all center-scoped actions.
 * Every controller/service that touches a center MUST authorize against the
 * caller's membership permission for that center (FR-013 from spec 011).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CenterSecurityService {

    private final CenterMembershipRepository membershipRepository;
    private final MaintenanceCenterRepository centerRepository;

    /**
     * Resolve the caller's active membership at the given center.
     * Returns the membership if the user is an active member (or the owner with an OWNER membership).
     */
    public Optional<CenterMembership> resolveMembership(Long centerId, User user) {
        // Check direct ownership first — owner always has implicit access
        Optional<MaintenanceCenter> center = centerRepository.findById(centerId);
        if (center.isPresent() && center.get().getOwner() != null
                && center.get().getOwner().getId().equals(user.getId())) {
            // Owner — look up their OWNER membership row
            return membershipRepository.findByCenterIdAndUserIdAndStatus(
                    centerId, user.getId(), MembershipStatus.ACTIVE);
        }

        // Staff — must have an active membership
        return membershipRepository.findByCenterIdAndUserIdAndStatus(
                centerId, user.getId(), MembershipStatus.ACTIVE);
    }

    /**
     * Require the caller to have a specific permission at the given center.
     * Throws AccessDeniedException if the user has no active membership or lacks the permission.
     */
    public CenterMembership requirePermission(Long centerId, User user, CenterPermission permission) {
        CenterMembership membership = resolveMembership(centerId, user)
                .orElseThrow(() -> new AccessDeniedException(
                        "You do not have access to this center"));

        if (!membership.getRole().hasPermission(permission)) {
            throw new AccessDeniedException(
                    "You do not have the required permission: " + permission);
        }

        return membership;
    }

    /**
     * Check if the caller has a specific permission at the given center.
     * Returns false (no exception) if the user lacks the permission.
     */
    public boolean hasPermission(Long centerId, User user, CenterPermission permission) {
        return resolveMembership(centerId, user)
                .map(m -> m.getRole().hasPermission(permission))
                .orElse(false);
    }

    /**
     * Require the caller to have an active membership at the given center (any role).
     * Used for read-only endpoints that don't need a specific permission.
     */
    public CenterMembership requireActiveMembership(Long centerId, User user) {
        return resolveMembership(centerId, user)
                .orElseThrow(() -> new AccessDeniedException(
                        "You do not have access to this center"));
    }
}
