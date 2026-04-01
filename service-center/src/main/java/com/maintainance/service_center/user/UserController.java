package com.maintainance.service_center.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "User profile management endpoints")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Retrieve the profile of the currently authenticated user")
    public ResponseEntity<UserResponse> getMyProfile(
            @AuthenticationPrincipal User caller) {
        UserResponse response = userService.getMyProfile(caller);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile", description = "Update the profile of the currently authenticated user")
    public ResponseEntity<UserResponse> updateMyProfile(
            @Valid @RequestBody UserRequest request,
            @AuthenticationPrincipal User caller) {
        UserResponse response = userService.updateMyProfile(request, caller);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/fcm-token")
    @Operation(summary = "Update FCM token", description = "Update the Firebase Cloud Messaging token for push notifications")
    public ResponseEntity<Void> updateFcmToken(
            @RequestParam String fcmToken,
            @AuthenticationPrincipal User caller) {
        userService.updateFcmToken(fcmToken, caller);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/me/location")
    @Operation(summary = "Update user location", description = "Update the user's last known location")
    public ResponseEntity<Void> updateLocation(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @AuthenticationPrincipal User caller) {
        userService.updateLocation(latitude, longitude, caller);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/me/notifications")
    @Operation(summary = "Update notification preferences", description = "Update user's notification preferences")
    public ResponseEntity<UserResponse> updateNotificationPreferences(
            @Valid @RequestBody NotificationPreferencesRequest request,
            @AuthenticationPrincipal User caller) {
        UserResponse response = userService.updateNotificationPreferences(request, caller);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/language")
    @Operation(summary = "Update language preference", description = "Update user's preferred language")
    public ResponseEntity<UserResponse> updateLanguage(
            @RequestParam Language language,
            @AuthenticationPrincipal User caller) {
        UserResponse response = userService.updateLanguage(language, caller);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me/profile-image")
    @Operation(summary = "Delete profile image", description = "Remove the user's profile image")
    public ResponseEntity<UserResponse> deleteProfileImage(
            @AuthenticationPrincipal User caller) {
        UserResponse response = userService.deleteProfileImage(caller);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/statistics")
    @Operation(summary = "Get user statistics", description = "Retrieve statistics for the current user")
    public ResponseEntity<UserStatisticsResponse> getUserStatistics(
            @AuthenticationPrincipal User caller) {
        UserStatisticsResponse response = userService.getUserStatistics(caller);
        return ResponseEntity.ok(response);
    }
}
