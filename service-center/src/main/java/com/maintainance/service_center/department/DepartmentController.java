package com.maintainance.service_center.department;

import com.maintainance.service_center.staff.CenterMembershipResponse;
import com.maintainance.service_center.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("centers/my/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> getDepartments(
            @AuthenticationPrincipal User caller) {
        return ResponseEntity.ok(departmentService.getDepartments(caller));
    }

    @PostMapping
    public ResponseEntity<DepartmentResponse> createDepartment(
            @Valid @RequestBody CreateDepartmentRequest request,
            @AuthenticationPrincipal User caller) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(departmentService.createDepartment(request, caller));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepartmentResponse> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDepartmentRequest request,
            @AuthenticationPrincipal User caller) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, request, caller));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateDepartment(
            @PathVariable Long id,
            @AuthenticationPrincipal User caller) {
        departmentService.deactivateDepartment(id, caller);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<CenterMembershipResponse>> getDepartmentMembers(
            @PathVariable Long id,
            @AuthenticationPrincipal User caller) {
        return ResponseEntity.ok(departmentService.getDepartmentMembers(id, caller));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<DepartmentResponse> addDepartmentMember(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentMembershipUpdateRequest request,
            @AuthenticationPrincipal User caller) {
        return ResponseEntity.ok(
                departmentService.addDepartmentMember(id, request.getMembershipId(), caller));
    }

    @DeleteMapping("/{departmentId}/members/{membershipId}")
    public ResponseEntity<Void> removeDepartmentMember(
            @PathVariable Long departmentId,
            @PathVariable Long membershipId,
            @AuthenticationPrincipal User caller) {
        departmentService.removeDepartmentMember(departmentId, membershipId, caller);
        return ResponseEntity.noContent().build();
    }
}
