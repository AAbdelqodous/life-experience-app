package com.maintainance.service_center.staff;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Staff Invitations")
public class InvitationController {

    private final StaffService staffService;

    @GetMapping("/invitations/{token}")
    public ResponseEntity<InvitationDetailsResponse> getInvitationDetails(@PathVariable String token) {
        return ResponseEntity.ok(staffService.getInvitationDetails(token));
    }

    @PostMapping("/invitations/{token}/accept")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CenterMembershipResponse> acceptInvitation(
            @PathVariable String token,
            @AuthenticationPrincipal com.maintainance.service_center.user.User user
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(staffService.acceptInvitation(token, user));
    }

    @PostMapping("/invitations/{token}/decline")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> declineInvitation(
            @PathVariable String token,
            @AuthenticationPrincipal com.maintainance.service_center.user.User user
    ) {
        staffService.declineInvitation(token, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/me/memberships")
    public ResponseEntity<List<MembershipSummaryResponse>> getUserMemberships(
            @AuthenticationPrincipal com.maintainance.service_center.user.User user
    ) {
        return ResponseEntity.ok(staffService.getUserMemberships(user));
    }
}
