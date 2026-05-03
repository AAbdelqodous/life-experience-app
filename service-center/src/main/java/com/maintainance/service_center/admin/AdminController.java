package com.maintainance.service_center.admin;

import com.maintainance.service_center.booking.BookingStatus;
import com.maintainance.service_center.category.ServiceCategoryRequest;
import com.maintainance.service_center.category.ServiceCategoryResponse;
import com.maintainance.service_center.complaint.ComplaintPriority;
import com.maintainance.service_center.complaint.ComplaintResponse;
import com.maintainance.service_center.complaint.ComplaintStatus;
import com.maintainance.service_center.complaint.ComplaintType;
import com.maintainance.service_center.user.ApprovalStatus;
import com.maintainance.service_center.user.UserResponse;
import com.maintainance.service_center.user.UserType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin-only endpoints for platform management")
public class AdminController {

    private final AdminService adminService;
    private final AdminAnalyticsService adminAnalyticsService;

    @GetMapping("/users/pending")
    @Operation(summary = "List pending center owners", description = "Returns all center owner accounts awaiting approval")
    public ResponseEntity<Page<UserResponse>> getPendingOwners(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdDate").ascending());
        return ResponseEntity.ok(adminService.getPendingOwners(pageable));
    }

    @PutMapping("/users/{id}/approve")
    @Operation(summary = "Approve center owner", description = "Grants access to an owner whose registration was pending")
    public ResponseEntity<UserResponse> approveOwner(@PathVariable Integer id) {
        return ResponseEntity.ok(adminService.approveOwner(id));
    }

    @PutMapping("/users/{id}/reject")
    @Operation(summary = "Reject center owner", description = "Permanently rejects a center owner registration")
    public ResponseEntity<UserResponse> rejectOwner(@PathVariable Integer id, @RequestBody(required = false) AdminRejectRequest request) {
        return ResponseEntity.ok(adminService.rejectOwner(id, request));
    }

    @GetMapping("/users")
    @Operation(summary = "List all users", description = "Returns all platform users, optionally filtered by type")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(required = false) UserType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        return ResponseEntity.ok(adminService.getAllUsers(type, pageable));
    }

    @GetMapping("/analytics/overview")
    @Operation(summary = "Get platform analytics overview", description = "Returns comprehensive platform statistics")
    public ResponseEntity<AdminAnalyticsResponse> getAnalyticsOverview() {
        return ResponseEntity.ok(adminAnalyticsService.getPlatformOverview());
    }

    // Center management endpoints
    @GetMapping("/centers")
    @Operation(summary = "List all centers", description = "Returns paginated list of all centers, optionally filtered by approval status and enabled status")
    public ResponseEntity<Page<AdminCenterResponse>> getAllCenters(
            @RequestParam(required = false) ApprovalStatus approvalStatus,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(adminService.getAllCenters(approvalStatus, enabled, pageable));
    }

    @GetMapping("/centers/{id}")
    @Operation(summary = "Get center by ID", description = "Returns full details of a specific center including owner info")
    public ResponseEntity<AdminCenterResponse> getCenterById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getCenterById(id));
    }

    @PutMapping("/centers/{id}/enable")
    @Operation(summary = "Enable center", description = "Enables a maintenance center")
    public ResponseEntity<AdminCenterResponse> enableCenter(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.enableCenter(id));
    }

    @PutMapping("/centers/{id}/disable")
    @Operation(summary = "Disable center", description = "Disables a maintenance center")
    public ResponseEntity<AdminCenterResponse> disableCenter(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.disableCenter(id));
    }

    // Platform-wide bookings view
    @GetMapping("/bookings")
    @Operation(summary = "List all bookings", description = "Returns paginated list of all bookings across all centers, optionally filtered")
    public ResponseEntity<Page<AdminBookingResponse>> getAllBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Long centerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(adminService.getAllBookings(status, centerId, startDate, endDate, pageable));
    }

    // Complaint management endpoints
    @GetMapping("/complaints")
    @Operation(summary = "List all complaints", description = "Returns paginated list of all complaints, optionally filtered")
    public ResponseEntity<Page<ComplaintResponse>> getAllComplaints(
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) ComplaintType type,
            @RequestParam(required = false) ComplaintPriority priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminService.getAllComplaints(status, type, priority, pageable));
    }

    @PutMapping("/complaints/{id}/status")
    @Operation(summary = "Update complaint status", description = "Updates the status of a complaint")
    public ResponseEntity<ComplaintResponse> updateComplaintStatus(
            @PathVariable Long id,
            @RequestBody @Valid AdminComplaintStatusRequest request) {
        return ResponseEntity.ok(adminService.updateComplaintStatus(id, request));
    }

    @PutMapping("/complaints/{id}/priority")
    @Operation(summary = "Update complaint priority", description = "Updates the priority of a complaint")
    public ResponseEntity<ComplaintResponse> updateComplaintPriority(
            @PathVariable Long id,
            @RequestBody @Valid AdminComplaintPriorityRequest request) {
        return ResponseEntity.ok(adminService.updateComplaintPriority(id, request));
    }

    // Category management endpoints
    @GetMapping("/categories")
    @Operation(summary = "List all categories", description = "Returns all service categories")
    public ResponseEntity<List<ServiceCategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(adminService.getAllCategories());
    }

    @PostMapping("/categories")
    @Operation(summary = "Create category", description = "Creates a new service category")
    public ResponseEntity<ServiceCategoryResponse> createCategory(@RequestBody @Valid ServiceCategoryRequest request) {
        return ResponseEntity.ok(adminService.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Update category", description = "Updates an existing service category")
    public ResponseEntity<ServiceCategoryResponse> updateCategory(
            @PathVariable Long id,
            @RequestBody @Valid ServiceCategoryRequest request) {
        return ResponseEntity.ok(adminService.updateCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "Delete category", description = "Deletes a service category if not referenced by any centers")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        adminService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
