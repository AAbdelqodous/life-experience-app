package com.maintainance.service_center.reroute;

import com.maintainance.service_center.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Spec 022 — re-route endpoints (POST /bookings/{id}/reroute, GET /bookings/{id}/reroute-history).
 * Class-level guard is the coarse cut; service layer enforces fine-grained permissions
 * (REROUTE_BOOKING_ASSIGNED for the assigned tech vs. REROUTE_BOOKING_ANY for managers).
 */
@RestController
@RequestMapping("bookings/{id}")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
@Tag(name = "Booking Re-Route")
@SecurityRequirement(name = "bearerAuth")
public class RerouteController {

    private final RerouteService rerouteService;

    @PostMapping("/reroute")
    @Operation(summary = "Re-route a booking to a different department",
            description = "Moves a non-terminal booking from its current department to another active, "
                    + "non-diagnostic department at the same center. Requires either an assigned-technician "
                    + "membership (REROUTE_BOOKING_ASSIGNED) or manager-level access (REROUTE_BOOKING_ANY).")
    public ResponseEntity<RerouteResponse> reroute(
            @PathVariable Long id,
            @Valid @RequestBody RerouteRequest request,
            @AuthenticationPrincipal User caller) {
        return ResponseEntity.ok(rerouteService.reroute(id, request, caller));
    }

    @GetMapping("/reroute-history")
    @Operation(summary = "List all re-route events for a booking",
            description = "Returns the chronological list of every re-route applied to the booking, "
                    + "oldest first. Managers see all history; technicians see only bookings they are "
                    + "or were assigned to.")
    public ResponseEntity<List<RerouteAuditResponse>> history(
            @PathVariable Long id,
            @AuthenticationPrincipal User caller) {
        return ResponseEntity.ok(rerouteService.getHistory(id, caller));
    }
}
