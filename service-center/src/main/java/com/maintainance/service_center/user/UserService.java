package com.maintainance.service_center.user;

import com.maintainance.service_center.config.FileStorageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Get current user's profile
     */
    public UserResponse getMyProfile(User user) {
        return toResponse(user);
    }

    /**
     * Update current user's profile
     */
    @Transactional
    public UserResponse updateMyProfile(UserRequest request, User user) {
        
        // Update basic information
        if (request.getFirstname() != null) {
            user.setFirstname(request.getFirstname());
        }
        if (request.getLastname() != null) {
            user.setLastname(request.getLastname());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAlternativePhone() != null) {
            user.setAlternativePhone(request.getAlternativePhone());
        }
        
        // Update profile information
        if (request.getProfileImageUrl() != null) {
            user.setProfileImageUrl(request.getProfileImageUrl());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        
        // Update address
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        
        // Update location
        if (request.getLastKnownLatitude() != null) {
            user.setLastKnownLatitude(request.getLastKnownLatitude());
        }
        if (request.getLastKnownLongitude() != null) {
            user.setLastKnownLongitude(request.getLastKnownLongitude());
        }
        
        // Update preferences
        if (request.getPreferredLanguage() != null) {
            user.setPreferredLanguage(request.getPreferredLanguage());
        }
        if (request.getPushNotificationsEnabled() != null) {
            user.setPushNotificationsEnabled(request.getPushNotificationsEnabled());
        }
        if (request.getEmailNotificationsEnabled() != null) {
            user.setEmailNotificationsEnabled(request.getEmailNotificationsEnabled());
        }
        if (request.getSmsNotificationsEnabled() != null) {
            user.setSmsNotificationsEnabled(request.getSmsNotificationsEnabled());
        }
        
        // Update FCM token for push notifications
        if (request.getFcmToken() != null) {
            user.setFcmToken(request.getFcmToken());
        }
        
        User savedUser = userRepository.save(user);
        log.info("User profile updated for user ID: {}", savedUser.getId());
        
        return toResponse(savedUser);
    }

    /**
     * Update user's FCM token for push notifications
     */
    @Transactional
    public void updateFcmToken(String fcmToken, User user) {
        user.setFcmToken(fcmToken);
        userRepository.save(user);
        log.info("FCM token updated for user ID: {}", user.getId());
    }

    /**
     * Update user's Expo push token for push notifications
     */
    @Transactional
    public void updatePushToken(String pushToken, User user) {
        user.setPushToken(pushToken);
        userRepository.save(user);
        log.info("Push token updated for user ID: {}", user.getId());
    }

    /**
     * Update user's location
     */
    @Transactional
    public void updateLocation(Double latitude, Double longitude, User user) {
        // Validate latitude range (-90 to 90)
        if (latitude != null && (latitude < -90 || latitude > 90)) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        // Validate longitude range (-180 to 180)
        if (longitude != null && (longitude < -180 || longitude > 180)) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
        
        user.setLastKnownLatitude(latitude);
        user.setLastKnownLongitude(longitude);
        userRepository.save(user);
        log.info("Location updated for user ID: {}", user.getId());
    }

    /**
     * Update user's notification preferences
     */
    @Transactional
    public UserResponse updateNotificationPreferences(NotificationPreferencesRequest request, User user) {
        
        if (request.getPushNotificationsEnabled() != null) {
            user.setPushNotificationsEnabled(request.getPushNotificationsEnabled());
        }
        if (request.getEmailNotificationsEnabled() != null) {
            user.setEmailNotificationsEnabled(request.getEmailNotificationsEnabled());
        }
        if (request.getSmsNotificationsEnabled() != null) {
            user.setSmsNotificationsEnabled(request.getSmsNotificationsEnabled());
        }
        
        User savedUser = userRepository.save(user);
        log.info("Notification preferences updated for user ID: {}", savedUser.getId());
        
        return toResponse(savedUser);
    }

    /**
     * Update user's language preference
     */
    @Transactional
    public UserResponse updateLanguage(Language language, User user) {
        user.setPreferredLanguage(language);
        User savedUser = userRepository.save(user);
        log.info("Language preference updated to {} for user ID: {}", language, savedUser.getId());
        
        return toResponse(savedUser);
    }

    /**
     * Delete user's profile image
     */
    @Transactional
    public UserResponse deleteProfileImage(User user) {
        user.setProfileImageUrl(null);
        User savedUser = userRepository.save(user);
        log.info("Profile image deleted for user ID: {}", savedUser.getId());
        
        return toResponse(savedUser);
    }

    /**
     * Upload user's profile image
     */
    @Transactional
    public UserResponse uploadProfileImage(MultipartFile file, User user) {
        // Validate and store the file
        String fileName = fileStorageService.storeFile(file);
        
        // Delete old profile image if exists
        if (user.getProfileImageUrl() != null) {
            String oldFileName = extractFileNameFromUrl(user.getProfileImageUrl());
            fileStorageService.deleteFile(oldFileName);
        }
        
        // Update user's profile image URL
        String imageUrl = "/uploads/" + fileName;
        user.setProfileImageUrl(imageUrl);
        User savedUser = userRepository.save(user);
        
        log.info("Profile image uploaded for user ID: {}", savedUser.getId());
        
        return toResponse(savedUser);
    }

    /**
     * Get user statistics
     */
    public UserStatisticsResponse getUserStatistics(User user) {
        return UserStatisticsResponse.builder()
                .totalBookings(user.getTotalBookings())
                .totalReviews(user.getTotalReviews())
                .helpfulReviews(user.getHelpfulReviews())
                .ownedCenters(user.getOwnedCenters() != null ? user.getOwnedCenters().size() : 0)
                .favorites(user.getFavorites() != null ? user.getFavorites().size() : 0)
                .build();
    }

    /**
     * Change user password
     */
    @Transactional
    public void changePassword(ChangePasswordRequest request, User user) {
        // Validate current password matches stored password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Failed password change attempt for user ID: {} - incorrect current password", user.getId());
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Validate new password is different from current password
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            log.warn("Failed password change attempt for user ID: {} - new password same as current", user.getId());
            throw new IllegalArgumentException("New password must be different from current password");
        }

        // Validate new password matches confirmation password
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.warn("Failed password change attempt for user ID: {} - passwords do not match", user.getId());
            throw new IllegalArgumentException("New password and confirmation password do not match");
        }

        // Encode and save new password
        String encodedNewPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPassword(encodedNewPassword);
        userRepository.save(user);

        log.info("Password changed successfully for user ID: {}", user.getId());
    }

    /**
     * Map User entity to UserResponse DTO
     */
    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .fullName(user.fullName())
                .dateOfBirth(user.getDateOfBirth())
                .email(user.getEmail())
                .phone(user.getPhone())
                .alternativePhone(user.getAlternativePhone())
                .profileImageUrl(user.getProfileImageUrl())
                .bio(user.getBio())
                .address(user.getAddress())
                .lastKnownLatitude(user.getLastKnownLatitude())
                .lastKnownLongitude(user.getLastKnownLongitude())
                .preferredLanguage(user.getPreferredLanguage())
                .pushNotificationsEnabled(user.getPushNotificationsEnabled())
                .emailNotificationsEnabled(user.getEmailNotificationsEnabled())
                .smsNotificationsEnabled(user.getSmsNotificationsEnabled())
                .userType(user.getUserType())
                .approvalStatus(user.getApprovalStatus())
                .accountLocked(user.isAccountLocked())
                .enabled(user.isEnabled())
                .emailVerifiedAt(user.getEmailVerifiedAt())
                .phoneVerifiedAt(user.getPhoneVerifiedAt())
                .totalBookings(user.getTotalBookings())
                .totalReviews(user.getTotalReviews())
                .helpfulReviews(user.getHelpfulReviews())
                .createdDate(user.getCreatedDate())
                .lastModifiedDate(user.getLastModifiedDate())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    /**
     * Extract file name from URL
     */
    private String extractFileNameFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        return url.substring(url.lastIndexOf('/') + 1);
    }
}
