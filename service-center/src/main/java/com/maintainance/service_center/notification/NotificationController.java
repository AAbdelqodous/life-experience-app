package com.maintainance.service_center.notification;

import com.maintainance.service_center.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification management endpoints")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get all notifications", description = "Retrieve paginated list of notifications for current user")
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            Pageable pageable,
            @AuthenticationPrincipal User caller) {
        Page<NotificationResponse> response = notificationService.getMyNotifications(pageable, caller);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread")
    @Operation(summary = "Get unread notifications", description = "Retrieve paginated list of unread notifications")
    public ResponseEntity<Page<NotificationResponse>> getUnreadNotifications(
            Pageable pageable,
            @AuthenticationPrincipal User caller) {
        Page<NotificationResponse> response = notificationService.getUnreadNotifications(pageable, caller);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/count/unread")
    @Operation(summary = "Get unread count", description = "Get count of unread notifications")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal User caller) {
        UnreadCountResponse response = notificationService.getUnreadCount(caller);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    @Operation(summary = "Get notification stats", description = "Get total and unread notification counts")
    public ResponseEntity<NotificationStatsResponse> getStats(
            @AuthenticationPrincipal User caller) {
        NotificationStatsResponse response = notificationService.getStats(caller);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notification by ID", description = "Retrieve a specific notification by ID")
    public ResponseEntity<NotificationResponse> getNotificationById(
            @PathVariable Long id,
            @AuthenticationPrincipal User caller) {
        NotificationResponse response = notificationService.getNotificationById(id, caller);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get notifications by type", description = "Retrieve notifications filtered by type")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByType(
            @PathVariable NotificationType type,
            @AuthenticationPrincipal User caller) {
        List<NotificationResponse> response = notificationService.getNotificationsByType(type, caller);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create notification", description = "Create a new notification")
    public ResponseEntity<NotificationResponse> createNotification(
            @Valid @RequestBody NotificationRequest request,
            @AuthenticationPrincipal User caller) {
        NotificationResponse response = notificationService.createNotification(request, caller);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark notification as read", description = "Mark a specific notification as read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal User caller) {
        NotificationResponse response = notificationService.markAsRead(id, caller);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications as read", description = "Mark all notifications as read")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal User caller) {
        notificationService.markAllAsRead(caller);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete notification", description = "Delete a specific notification")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal User caller) {
        notificationService.deleteNotification(id, caller);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/read")
    @Operation(summary = "Delete read notifications", description = "Delete all read notifications")
    public ResponseEntity<Void> deleteReadNotifications(
            @AuthenticationPrincipal User caller) {
        notificationService.deleteReadNotifications(caller);
        return ResponseEntity.ok().build();
    }
}
