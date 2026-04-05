package com.maintainance.service_center.complaint;

import com.maintainance.service_center.common.PageResponse;
import com.maintainance.service_center.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("complaints")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ComplaintController {

    private final ComplaintService complaintService;

    @PostMapping
    @Operation(summary = "File a new complaint")
    public ResponseEntity<ComplaintResponse> fileComplaint(
            @Valid @RequestBody ComplaintRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(complaintService.fileComplaint(request, user));
    }

    @GetMapping
    @Operation(summary = "Get current user's complaints")
    public ResponseEntity<PageResponse<ComplaintResponse>> getMyComplaints(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder
    ) {
        return ResponseEntity.ok(complaintService.getMyComplaints(user, page, size, sortBy, sortOrder));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get complaint stats for current user")
    public ResponseEntity<ComplaintStatsResponse> getMyStats(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(complaintService.getMyStats(user));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a specific complaint by ID")
    public ResponseEntity<ComplaintResponse> getComplaintById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(complaintService.getComplaintById(id, user));
    }
}
