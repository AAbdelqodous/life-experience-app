package com.maintainance.service_center.user;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * Get current user's profile
     */
    public UserResponse getMyProfile(User user) {
        return mapToResponse(user);
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
        
        return mapToResponse(savedUser);
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
        
        return mapToResponse(savedUser);
    }

    /**
     * Update user's language preference
     */
    @Transactional
    public UserResponse updateLanguage(Language language, User user) {
        user.setPreferredLanguage(language);
        User savedUser = userRepository.save(user);
        log.info("Language preference updated to {} for user ID: {}", language, savedUser.getId());
        
        return mapToResponse(savedUser);
    }

    /**
     * Delete user's profile image
     */
    @Transactional
    public UserResponse deleteProfileImage(User user) {
        user.setProfileImageUrl(null);
        User savedUser = userRepository.save(user);
        log.info("Profile image deleted for user ID: {}", savedUser.getId());
        
        return mapToResponse(savedUser);
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
     * Map User entity to UserResponse DTO
     */
    private UserResponse mapToResponse(User user) {
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
}
