package com.maintainance.service_center.staff;

import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.email.EmailService;
import com.maintainance.service_center.handler.BusinessErrorCodes;
import com.maintainance.service_center.role.RoleRepository;
import com.maintainance.service_center.user.TokenRepository;
import com.maintainance.service_center.user.User;
import com.maintainance.service_center.user.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final RoleRepository roleRepository;
    private final EmailService emailService;
    private final TokenRepository tokenRepository;
    private final CenterSecurityService centerSecurity;

    @Value("${application.frontend-url}")
    private String frontendUrl;

    @Value("${app.staff.max-members-per-center:50}")
    private int maxMembersPerCenter;

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

        // Cannot invite yourself (3002)
        if (request.getTargetEmail().equalsIgnoreCase(caller.getEmail())) {
            throw new StaffOperationException(BusinessErrorCodes.CANNOT_INVITE_SELF);
        }

        // Cannot invite platform admins (3003)
        userRepository.findByEmail(request.getTargetEmail()).ifPresent(targetUser -> {
            if (targetUser.getUserType() == com.maintainance.service_center.user.UserType.ADMIN
                    || targetUser.getUserType() == com.maintainance.service_center.user.UserType.SUPER_ADMIN) {
                throw new StaffOperationException(BusinessErrorCodes.CANNOT_INVITE_ADMIN);
            }
        });

        checkCanInviteStaff(center, caller, request.getTargetRole());

        // Enforce max staff cap (NFR-007) — code 3001
        long activeOrInvitedCount = membershipRepository.countByCenterIdAndStatusIn(
                center.getId(), List.of(MembershipStatus.ACTIVE, MembershipStatus.INVITED));
        if (activeOrInvitedCount >= maxMembersPerCenter) {
            throw new StaffOperationException(BusinessErrorCodes.STAFF_LIMIT_REACHED,
                    "Center has reached the maximum number of staff members (" + maxMembersPerCenter + ")");
        }

        // Check if there's already a pending invitation for this email.
        // Expire any stale ones first so a re-invite is allowed after the window passes.
        List<StaffInvitation> existingInvitations = invitationRepository.findByCenterIdAndTargetEmailAndStatusIn(
                center.getId(), request.getTargetEmail(), List.of(InvitationStatus.PENDING));

        LocalDateTime now = LocalDateTime.now();
        List<StaffInvitation> stillPending = existingInvitations.stream()
                .filter(inv -> {
                    if (inv.getExpiresAt().isBefore(now)) {
                        inv.setStatus(InvitationStatus.EXPIRED);
                        invitationRepository.save(inv);
                        return false;
                    }
                    return true;
                })
                .toList();

        if (!stillPending.isEmpty()) {
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

        String inviteLink = frontendUrl + "/accept-invite?token=" + invitation.getToken();
        try {
            emailService.sendInvitationEmail(
                    request.getTargetEmail(),
                    inviteLink,
                    center.getNameEn(),
                    invitation.getTargetRole().getEnglish()
            );
        } catch (MessagingException e) {
            log.error("Failed to send invitation email to {}", request.getTargetEmail(), e);
        }

        return invitation.getId();
    }

    @Transactional
    public CenterMembershipResponse updateMemberRole(Long membershipId, UpdateRoleRequest request, User caller) {
        CenterMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new EntityNotFoundException("Membership not found with id: " + membershipId));

        MaintenanceCenter center = membership.getCenter();
        CenterMembership callerMembership = checkMembershipPermission(center, caller);

        // Cannot change an OWNER's role
        if (membership.getRole() == CenterRole.OWNER) {
            throw new StaffOperationException(BusinessErrorCodes.CANNOT_REMOVE_OWNER,
                    "Cannot change Owner role");
        }

        // Cannot assign OWNER role to someone else
        if (request.getRole() == CenterRole.OWNER) {
            throw new IllegalArgumentException("Cannot assign OWNER role via role update");
        }

        // Check caller can manage this member's CURRENT role
        if (!canManageMember(membership, caller)) {
            throw new StaffOperationException(BusinessErrorCodes.INSUFFICIENT_ROLE_SCOPE,
                    "You do not have permission to manage this member");
        }

        // EC-7: Check caller can assign the TARGET role.
        // Changing to BRANCH_MANAGER or ACCOUNTANT requires MANAGE_ALL_STAFF (Owner only).
        if (!callerMembership.getRole().hasPermission(CenterPermission.MANAGE_ALL_STAFF)) {
            if (request.getRole() == CenterRole.BRANCH_MANAGER || request.getRole() == CenterRole.ACCOUNTANT) {
                throw new StaffOperationException(BusinessErrorCodes.INSUFFICIENT_ROLE_SCOPE,
                        "Only the Owner can assign Branch Manager or Accountant roles");
            }
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

        // Cannot remove the owner (3006)
        if (membership.getRole() == CenterRole.OWNER) {
            throw new StaffOperationException(BusinessErrorCodes.CANNOT_REMOVE_OWNER);
        }

        // Owners cannot remove themselves via this endpoint
        if (membership.getUser().getId().equals(caller.getId())) {
            throw new StaffOperationException(BusinessErrorCodes.CANNOT_REMOVE_OWNER,
                    "Owners cannot remove themselves");
        }

        // Check if caller can manage this member (3007)
        if (!canManageMember(membership, caller)) {
            throw new StaffOperationException(BusinessErrorCodes.INSUFFICIENT_ROLE_SCOPE,
                    "You do not have permission to manage this member");
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

        // Cannot suspend the owner
        if (membership.getRole() == CenterRole.OWNER) {
            throw new StaffOperationException(BusinessErrorCodes.CANNOT_REMOVE_OWNER,
                    "Cannot suspend the center owner");
        }

        // Check if caller can manage this member (3007)
        if (!canManageMember(membership, caller)) {
            throw new StaffOperationException(BusinessErrorCodes.INSUFFICIENT_ROLE_SCOPE,
                    "You do not have permission to manage this member");
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

        // Check if caller can manage this member (3007)
        if (!canManageMember(membership, caller)) {
            throw new StaffOperationException(BusinessErrorCodes.INSUFFICIENT_ROLE_SCOPE,
                    "You do not have permission to manage this member");
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

        // Owner cannot leave their own center (3006)
        if (activeMembership.getRole() == CenterRole.OWNER) {
            throw new StaffOperationException(BusinessErrorCodes.CANNOT_REMOVE_OWNER,
                    "Owners cannot leave. Transfer ownership first.");
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
        // Check caller can invite for this role (enforces scope — BM cannot resend BM/Accountant invites)
        checkCanInviteStaff(center, caller, invitation.getTargetRole());

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new StaffOperationException(BusinessErrorCodes.INVITATION_NOT_PENDING,
                    "Can only resend pending invitations");
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

        // Validate invitation status (3004)
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new StaffOperationException(BusinessErrorCodes.INVITATION_NOT_PENDING,
                    "Invitation is not pending. Status: " + invitation.getStatus());
        }

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new StaffOperationException(BusinessErrorCodes.INVITATION_NOT_PENDING,
                    "Invitation has expired");
        }

        // Email must match (3005)
        if (!invitation.getTargetEmail().equalsIgnoreCase(user.getEmail())) {
            throw new StaffOperationException(BusinessErrorCodes.INVITATION_EMAIL_MISMATCH);
        }

        // Check if user is already a member
        if (membershipRepository.existsByCenterIdAndUserId(invitation.getCenter().getId(), user.getId())) {
            throw new IllegalArgumentException("You are already a member of this center");
        }

        // Auto-activate user as belt-and-suspenders
        if (!user.isEnabled()) {
            user.setEnabled(true);
            tokenRepository.deleteAllByUser(user);
        }

        // Mark as STAFF and assign ROLE_STAFF regardless of previous user type
        var staffRole = roleRepository.findByName("ROLE_STAFF")
                .orElseThrow(() -> new IllegalStateException("ROLE_STAFF was not initialized"));
        user.setUserType(com.maintainance.service_center.user.UserType.STAFF);
        user.setRoles(List.of(staffRole));
        userRepository.save(user);
        log.info("Set user {} type to STAFF and assigned ROLE_STAFF on invitation acceptance", user.getId());

        // Create membership — cache the user's name + email so historical
        // attribution (FR-011) survives later deletion of the User row.
        CenterMembership membership = CenterMembership.builder()
                .user(user)
                .userFirstname(user.getFirstname())
                .userLastname(user.getLastname())
                .userEmail(user.getEmail())
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
            throw new StaffOperationException(BusinessErrorCodes.INVITATION_NOT_PENDING,
                    "Invitation is not pending. Status: " + invitation.getStatus());
        }

        if (!invitation.getTargetEmail().equalsIgnoreCase(user.getEmail())) {
            throw new StaffOperationException(BusinessErrorCodes.INVITATION_EMAIL_MISMATCH);
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
        // Try owner path first
        var owned = centerRepository.findFirstByOwnerId(caller.getId());
        if (owned.isPresent()) {
            return owned.get();
        }

        // Fall back to active membership (Branch Manager, etc.)
        return membershipRepository.findByUserIdAndStatus(caller.getId(), MembershipStatus.ACTIVE)
                .stream()
                .findFirst()
                .map(CenterMembership::getCenter)
                .orElseThrow(() -> new EntityNotFoundException("No center found for this user"));
    }

    /**
     * Check that the caller has at least staff-viewing permission at this center.
     * Owners and Branch Managers can view the staff list.
     */
    private CenterMembership checkMembershipPermission(MaintenanceCenter center, User caller) {
        // OWNER always has MANAGE_ALL_STAFF; BRANCH_MANAGER has MANAGE_NON_MANAGER_STAFF
        // Either permission is sufficient to VIEW the staff list
        CenterMembership membership = centerSecurity.resolveMembership(center.getId(), caller)
                .orElseThrow(() -> new AccessDeniedException("You do not have access to this center"));
        if (!membership.getRole().hasPermission(CenterPermission.MANAGE_ALL_STAFF)
                && !membership.getRole().hasPermission(CenterPermission.MANAGE_NON_MANAGER_STAFF)) {
            throw new AccessDeniedException("You do not have permission to manage this center's staff");
        }
        return membership;
    }

    /**
     * Check that the caller can invite staff. OWNER can invite anyone.
     * BRANCH_MANAGER can only invite TECHNICIAN and RECEPTIONIST.
     */
    private CenterMembership checkCanInviteStaff(MaintenanceCenter center, User caller, CenterRole targetRole) {
        CenterMembership callerMembership = checkMembershipPermission(center, caller);

        if (callerMembership.getRole().hasPermission(CenterPermission.MANAGE_ALL_STAFF)) {
            return callerMembership;
        }

        // MANAGE_NON_MANAGER_STAFF: can only invite Technicians and Receptionists
        // (not BMs, Accountants, or Owners — amendment D)
        if (callerMembership.getRole().hasPermission(CenterPermission.MANAGE_NON_MANAGER_STAFF)) {
            if (targetRole != CenterRole.TECHNICIAN && targetRole != CenterRole.RECEPTIONIST) {
                throw new StaffOperationException(BusinessErrorCodes.INSUFFICIENT_ROLE_SCOPE,
                        "Branch Managers can only invite Technicians and Receptionists");
            }
            return callerMembership;
        }

        throw new StaffOperationException(BusinessErrorCodes.INSUFFICIENT_ROLE_SCOPE,
                "You do not have permission to invite staff");
    }

    private boolean canManageMember(CenterMembership membership, User caller) {
        CenterMembership callerMembership = centerSecurity.resolveMembership(
                membership.getCenter().getId(), caller).orElse(null);
        if (callerMembership == null) {
            return false;
        }

        // OWNER (MANAGE_ALL_STAFF) can manage everyone except themselves
        if (callerMembership.getRole().hasPermission(CenterPermission.MANAGE_ALL_STAFF)) {
            return true;
        }

        // BRANCH_MANAGER (MANAGE_NON_MANAGER_STAFF) can manage Technicians and Receptionists only.
        // Accountants are excluded per spec 011 amendment D — they have revenue access.
        if (callerMembership.getRole().hasPermission(CenterPermission.MANAGE_NON_MANAGER_STAFF)) {
            return membership.getRole() == CenterRole.TECHNICIAN
                    || membership.getRole() == CenterRole.RECEPTIONIST;
        }

        return false;
    }

    private CenterMembershipResponse toMembershipResponse(CenterMembership membership) {
        // Prefer cached attribution; fall back to the live User FK for rows
        // written before the cache columns existed.
        User user = membership.getUser();
        String firstname = membership.getUserFirstname() != null
                ? membership.getUserFirstname() : (user != null ? user.getFirstname() : null);
        String lastname = membership.getUserLastname() != null
                ? membership.getUserLastname() : (user != null ? user.getLastname() : null);
        String email = membership.getUserEmail() != null
                ? membership.getUserEmail() : (user != null ? user.getEmail() : null);
        return CenterMembershipResponse.builder()
                .id(membership.getId())
                .userId(user != null ? user.getId() : null)
                .userFirstname(firstname)
                .userLastname(lastname)
                .userEmail(email)
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
                .targetEmail(invitation.getTargetEmail())
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
