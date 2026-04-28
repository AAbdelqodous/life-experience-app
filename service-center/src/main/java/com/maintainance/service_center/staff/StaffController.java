package com.maintainance.service_center.staff;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("centers/my/staff")
@RequiredArgsConstructor
@Tag(name = "Center Staff Management")
public class StaffController {

    private final StaffService staffService;

    @GetMapping
    public ResponseEntity<Page<CenterMembershipResponse>> getStaff(
            @RequestParam(required = false) MembershipStatus status,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal com.maintainance.service_center.user.User caller
    ) {
        // Get the caller's center
        com.maintainance.service_center.center.MaintenanceCenter center = 
                staffService.getCallerCenter(caller);
        
        return ResponseEntity.ok(staffService.getStaff(center.getId(), status, pageable, caller));
    }

    @PostMapping("/invite")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Map<String, Long>> inviteStaff(
            @RequestBody @Valid InviteStaffRequest request,
            @AuthenticationPrincipal com.maintainance.service_center.user.User caller
    ) {
        Long invitationId = staffService.inviteStaff(request, caller);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("invitationId", invitationId));
    }

    @PutMapping("/{membershipId}")
    public ResponseEntity<CenterMembershipResponse> updateMemberRole(
            @PathVariable Long membershipId,
            @RequestBody @Valid UpdateRoleRequest request,
            @AuthenticationPrincipal com.maintainance.service_center.user.User caller
    ) {
        return ResponseEntity.ok(staffService.updateMemberRole(membershipId, request, caller));
    }

    @DeleteMapping("/{membershipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> removeMember(
            @PathVariable Long membershipId,
            @AuthenticationPrincipal com.maintainance.service_center.user.User caller
    ) {
        staffService.removeMember(membershipId, caller);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{membershipId}/suspend")
    public ResponseEntity<CenterMembershipResponse> suspendMember(
            @PathVariable Long membershipId,
            @AuthenticationPrincipal com.maintainance.service_center.user.User caller
    ) {
        return ResponseEntity.ok(staffService.suspendMember(membershipId, caller));
    }

    @PutMapping("/{membershipId}/reinstate")
    public ResponseEntity<CenterMembershipResponse> reinstateMember(
            @PathVariable Long membershipId,
            @AuthenticationPrincipal com.maintainance.service_center.user.User caller
    ) {
        return ResponseEntity.ok(staffService.reinstateMember(membershipId, caller));
    }

    @DeleteMapping("/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> leaveCenter(
            @AuthenticationPrincipal com.maintainance.service_center.user.User caller
    ) {
        staffService.leaveCenter(caller);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/invitations/{id}/resend")
    public ResponseEntity<Map<String, Long>> resendInvitation(
            @PathVariable Long id,
            @AuthenticationPrincipal com.maintainance.service_center.user.User caller
    ) {
        Long invitationId = staffService.resendInvitation(id, caller);
        return ResponseEntity.ok(Map.of("invitationId", invitationId));
    }
}
