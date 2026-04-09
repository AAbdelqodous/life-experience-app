package com.maintainance.service_center.admin;

import com.maintainance.service_center.user.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin-only endpoints for platform management")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users/pending")
    @Secured("ROLE_ADMIN")
    @Operation(summary = "List pending center owners", description = "Returns all center owner accounts awaiting approval")
    public ResponseEntity<Page<UserResponse>> getPendingOwners(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdDate").ascending());
        return ResponseEntity.ok(adminService.getPendingOwners(pageable));
    }

    @PutMapping("/users/{id}/approve")
    @Secured("ROLE_ADMIN")
    @Operation(summary = "Approve center owner", description = "Grants access to an owner whose registration was pending")
    public ResponseEntity<UserResponse> approveOwner(@PathVariable Integer id) {
        return ResponseEntity.ok(adminService.approveOwner(id));
    }

    @PutMapping("/users/{id}/reject")
    @Secured("ROLE_ADMIN")
    @Operation(summary = "Reject center owner", description = "Permanently rejects a center owner registration")
    public ResponseEntity<UserResponse> rejectOwner(@PathVariable Integer id) {
        return ResponseEntity.ok(adminService.rejectOwner(id));
    }
}
