package com.maintainance.service_center.center;

import com.maintainance.service_center.staff.CenterMembershipRepository;
import com.maintainance.service_center.staff.MembershipStatus;
import com.maintainance.service_center.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CenterResolverService {

    private final MaintenanceCenterRepository centerRepository;
    private final CenterMembershipRepository membershipRepository;

    /** Returns the center owned by or actively staffed by the given user. */
    public MaintenanceCenter resolveCenter(User user) {
        var owned = centerRepository.findFirstByOwnerId(user.getId());
        if (owned.isPresent()) return owned.get();

        return membershipRepository
                .findByUserIdAndStatus(user.getId(), MembershipStatus.ACTIVE)
                .stream()
                .findFirst()
                .map(m -> centerRepository.findById(m.getCenter().getId())
                        .orElseThrow(() -> new EntityNotFoundException("Center not found")))
                .orElseThrow(() -> new EntityNotFoundException("No center found for this account"));
    }

    /** Throws AccessDeniedException if the user is neither the owner nor an active member of the center. */
    public void assertAccess(Long centerId, User user) {
        boolean isOwner = centerRepository.findById(centerId)
                .map(c -> c.getOwner() != null && c.getOwner().getId().equals(user.getId()))
                .orElse(false);
        if (isOwner) return;

        boolean isActiveMember = membershipRepository
                .findByCenterIdAndUserId(centerId, user.getId())
                .map(m -> m.getStatus() == MembershipStatus.ACTIVE)
                .orElse(false);

        if (!isActiveMember) {
            throw new AccessDeniedException("You do not have permission to access this center");
        }
    }
}