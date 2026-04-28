package com.maintainance.service_center.staff;

import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.handler.BusinessErrorCodes;
import com.maintainance.service_center.user.User;
import com.maintainance.service_center.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffService {

    private final CenterMembershipRepository membershipRepository;
    private final StaffInvitationRepository invitationRepository;
    private final MaintenanceCenterRepository centerRepository;
    private final UserRepository userRepository;

    public Page<CenterMembershipResponse> getStaff(Long centerId, MembershipStatus status, Pageable pageable, User caller) {
        MaintenanceCenter center = getActiveCenter(centerId);
        checkMembershipPermission(center, caller);

        Page<CenterMembership> memberships;
        if (status != null) {
            memberships = membershipRepository.findByCenterIdAndStatus(centerId, status, pageable);
        } else {
            memberships = membershipRepository.findByCenterId(centerId, pageable);
        }

        return memberships.map(this::toMembershipResponse);
    }

    @Transactional
    public Long inviteStaff(InviteStaffRequest request, User caller) {
        MaintenanceCenter center = getCallerCenter(caller);
        checkCanInviteStaff(center, caller);

        // Check if there's already a pending invitation for this email
        List<InvitationStatus> pendingStatuses = List.of(InvitationStatus.PENDING);
        List<StaffInvitation> existingInvitations = invitationRepository.findByCenterIdAndTargetEmailAndStatusIn(
                center.getId(), request.getTargetEmail(), pendingStatuses);

        if (!existingInvitations.isEmpty()) {
            throw new IllegalArgumentException("A pending invitation already exists for this email");
        }

        // Check if user is already a member
        if (membershipRepository.existsByCenterIdAndUserId(center.getId(), 
                userRepository.findByEmail(request.getTargetEmail()).map(User::getId).orElse(null))) {
            throw new IllegalArgumentException("This user is already a member of this center");
        }

        StaffInvitation invitation = StaffInvitation.builder()
                .center(center)
                .invitedBy(caller)
                .targetEmail(request.getTargetEmail())
                .targetRole(request.getTargetRole())
                .build();

        invitation = invitationRepository.save(invitation);
        log.info("Created staff invitation id={} for email={} to center={} by user={}", 
                invitation.getId(), request.getTargetEmail(), center.getId(), caller.getId());

        return invitation.getId();
    }

    @Transactional
    public CenterMembershipResponse updateMemberRole(Long membershipId, UpdateRoleRequest request, User caller) {
        CenterMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new EntityNotFoundException("Membership not found with id: " + membershipId));

        MaintenanceCenter center = membership.getCenter();
        checkMembershipPermission(center, caller);

        // Check if caller can manage this member
        if (!canManageMember(membership, caller)) {
            throw new AccessDeniedException("You do not have permission to manage this member");
        }

        // Check if trying to assign OWNER role (only one owner allowed)
        if (request.getRole() == CenterRole.OWNER && !membership.getRole().equals(CenterRole.OWNER)) {
            throw new IllegalArgumentException("Cannot assign OWNER role. Only the original owner can have this role");
        }

        membership.setRole(request.getRole());
        membership = membershipRepository.save(membership);

        log.info("Updated membership id={} role to {} by user={}", membershipId, request.getRole(), caller.getId());
        return toMembershipResponse(membership);
    }

    @Transactional
    public void removeMember(Long membershipId, User caller) {
        CenterMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new EntityNotFoundException("Membership not found with id: " + membershipId));

        MaintenanceCenter center = membership.getCenter();
        checkMembershipPermission(center, caller);

        // Check if caller can manage this member
        if (!canManageMember(membership, caller)) {
            throw new AccessDeniedException("You do not have permission to manage this member");
        }

        // Cannot remove the owner
        if (membership.getRole() == CenterRole.OWNER) {
            throw new IllegalArgumentException("Cannot remove the center owner");
        }

        membership.setStatus(MembershipStatus.REMOVED);
        membershipRepository.save(membership);

        log.info("Removed membership id={} by user={}", membershipId, caller.getId());
    }

    @Transactional
    public CenterMembershipResponse suspendMember(Long membershipId, User caller) {
        CenterMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new EntityNotFoundException("Membership not found with id: " + membershipId));

        MaintenanceCenter center = membership.getCenter();
        checkMembershipPermission(center, caller);

        // Check if caller can manage this member
        if (!canManageMember(membership, caller)) {
            throw new AccessDeniedException("You do not have permission to manage this member");
        }

        // Cannot suspend the owner
        if (membership.getRole() == CenterRole.OWNER) {
            throw new IllegalArgumentException("Cannot suspend the center owner");
        }

        membership.setStatus(MembershipStatus.SUSPENDED);
        membership = membershipRepository.save(membership);

        log.info("Suspended membership id={} by user={}", membershipId, caller.getId());
        return toMembershipResponse(membership);
    }

    @Transactional
    public CenterMembershipResponse reinstateMember(Long membershipId, User caller) {
        CenterMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new EntityNotFoundException("Membership not found with id: " + membershipId));

        MaintenanceCenter center = membership.getCenter();
        checkMembershipPermission(center, caller);

        // Check if caller can manage this member
        if (!canManageMember(membership, caller)) {
            throw new AccessDeniedException("You do not have permission to manage this member");
        }

        membership.setStatus(MembershipStatus.ACTIVE);
        membership = membershipRepository.save(membership);

        log.info("Reinstated membership id={} by user={}", membershipId, caller.getId());
        return toMembershipResponse(membership);
    }

    @Transactional
    public void leaveCenter(User caller) {
        List<CenterMembership> memberships = membershipRepository.findByUserId(caller.getId());
        
        CenterMembership activeMembership = memberships.stream()
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("No active membership found"));

        // Owner cannot leave their own center
        if (activeMembership.getRole() == CenterRole.OWNER) {
            throw new IllegalArgumentException("Owner cannot leave the center. Please transfer ownership first");
        }

        activeMembership.setStatus(MembershipStatus.REMOVED);
        membershipRepository.save(activeMembership);

        log.info("User {} left center {}", caller.getId(), activeMembership.getCenter().getId());
    }

    @Transactional
    public Long resendInvitation(Long invitationId, User caller) {
        StaffInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new EntityNotFoundException("Invitation not found with id: " + invitationId));

        MaintenanceCenter center = invitation.getCenter();
        checkMembershipPermission(center, caller);

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalArgumentException("Can only resend pending invitations");
        }

        // Update expiration time
        invitation.setExpiresAt(LocalDateTime.now().plusHours(48));
        invitation = invitationRepository.save(invitation);

        log.info("Resent invitation id={} for email={}", invitationId, invitation.getTargetEmail());
        return invitation.getId();
    }

    public InvitationDetailsResponse getInvitationDetails(String token) {
        StaffInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Invalid invitation token"));

        // Check if expired
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            if (invitation.getStatus() == InvitationStatus.PENDING) {
                invitation.setStatus(InvitationStatus.EXPIRED);
                invitationRepository.save(invitation);
            }
            throw new IllegalArgumentException("Invitation has expired");
        }

        return toInvitationDetailsResponse(invitation);
    }

    @Transactional
    public CenterMembershipResponse acceptInvitation(String token, User user) {
        StaffInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Invalid invitation token"));

        // Validate invitation
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalArgumentException("Invitation is not pending. Status: " + invitation.getStatus());
        }

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new IllegalArgumentException("Invitation has expired");
        }

        if (!invitation.getTargetEmail().equalsIgnoreCase(user.getEmail())) {
            throw new IllegalArgumentException("This invitation is for a different email address");
        }

        // Check if user is already a member
        if (membershipRepository.existsByCenterIdAndUserId(invitation.getCenter().getId(), user.getId())) {
            throw new IllegalArgumentException("You are already a member of this center");
        }

        // Create membership
        CenterMembership membership = CenterMembership.builder()
                .user(user)
                .center(invitation.getCenter())
                .role(invitation.getTargetRole())
                .status(MembershipStatus.ACTIVE)
                .invitedBy(invitation.getInvitedBy())
                .activatedAt(LocalDateTime.now())
                .build();

        membership = membershipRepository.save(membership);

        // Mark invitation as redeemed
        invitation.setStatus(InvitationStatus.REDEEMED);
        invitationRepository.save(invitation);

        log.info("User {} accepted invitation id={} and joined center {}", 
                user.getId(), invitation.getId(), invitation.getCenter().getId());

        return toMembershipResponse(membership);
    }

    @Transactional
    public void declineInvitation(String token, User user) {
        StaffInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Invalid invitation token"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalArgumentException("Invitation is not pending. Status: " + invitation.getStatus());
        }

        if (!invitation.getTargetEmail().equalsIgnoreCase(user.getEmail())) {
            throw new IllegalArgumentException("This invitation is for a different email address");
        }

        invitation.setStatus(InvitationStatus.DECLINED);
        invitationRepository.save(invitation);

        log.info("User {} declined invitation id={}", user.getId(), invitation.getId());
    }

    public List<MembershipSummaryResponse> getUserMemberships(User user) {
        List<CenterMembership> memberships = membershipRepository.findByUserId(user.getId());
        List<MembershipSummaryResponse> responses = new ArrayList<>();

        for (CenterMembership membership : memberships) {
            MaintenanceCenter center = membership.getCenter();
            responses.add(MembershipSummaryResponse.builder()
                    .centerId(center.getId())
                    .centerNameAr(center.getNameAr())
                    .centerNameEn(center.getNameEn())
                    .centerLogoUrl(center.getLogoUrl())
                    .role(membership.getRole())
                    .roleAr(membership.getRole().getArabic())
                    .roleEn(membership.getRole().getEnglish())
                    .status(membership.getStatus())
                    .build());
        }

        return responses;
    }

    private MaintenanceCenter getActiveCenter(Long centerId) {
        MaintenanceCenter center = centerRepository.findById(centerId)
                .orElseThrow(() -> new EntityNotFoundException("Center not found with id: " + centerId));
        if (!center.getIsActive()) {
            throw new EntityNotFoundException("Center not found with id: " + centerId);
        }
        return center;
    }

    public MaintenanceCenter getCallerCenter(User caller) {
        return centerRepository.findFirstByOwnerId(caller.getId())
                .orElseThrow(() -> new EntityNotFoundException("No center found for this owner"));
    }

    private void checkMembershipPermission(MaintenanceCenter center, User caller) {
        if (!center.getOwner().getId().equals(caller.getId())) {
            throw new AccessDeniedException("You do not have permission to manage this center's staff");
        }
    }

    private void checkCanInviteStaff(MaintenanceCenter center, User caller) {
        checkMembershipPermission(center, caller);
    }

    private boolean canManageMember(CenterMembership membership, User caller) {
        // Owner can manage everyone
        if (membership.getCenter().getOwner().getId().equals(caller.getId())) {
            return true;
        }

        // Check if caller is a branch manager
        CenterMembership callerMembership = membershipRepository
                .findByCenterIdAndUserId(membership.getCenter().getId(), caller.getId())
                .orElse(null);

        if (callerMembership == null || callerMembership.getRole() != CenterRole.BRANCH_MANAGER) {
            return false;
        }

        // Branch manager can only manage non-manager staff
        return membership.getRole() != CenterRole.OWNER && 
               membership.getRole() != CenterRole.BRANCH_MANAGER;
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

    private InvitationDetailsResponse toInvitationDetailsResponse(StaffInvitation invitation) {
        return InvitationDetailsResponse.builder()
                .id(invitation.getId())
                .centerNameAr(invitation.getCenter().getNameAr())
                .centerNameEn(invitation.getCenter().getNameEn())
                .centerLogoUrl(invitation.getCenter().getLogoUrl())
                .inviterName(invitation.getInvitedBy().fullName())
                .targetRole(invitation.getTargetRole())
                .roleAr(invitation.getTargetRole().getArabic())
                .roleEn(invitation.getTargetRole().getEnglish())
                .expiresAt(invitation.getExpiresAt())
                .status(invitation.getStatus())
                .build();
    }

    public User getAuthenticatedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
