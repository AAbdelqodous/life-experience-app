package com.maintainance.service_center.user;

import com.maintainance.service_center.address.Address;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    
    private Integer id;
    
    private String firstname;
    
    private String lastname;
    
    private String fullName;
    
    private LocalDate dateOfBirth;
    
    private String email;
    
    private String phone;
    
    private String alternativePhone;
    
    private String profileImageUrl;
    
    private String bio;
    
    private Address address;
    
    private Double lastKnownLatitude;
    
    private Double lastKnownLongitude;
    
    private Language preferredLanguage;
    
    private Boolean pushNotificationsEnabled;
    
    private Boolean emailNotificationsEnabled;
    
    private Boolean smsNotificationsEnabled;
    
    private UserType userType;

    private ApprovalStatus approvalStatus;

    private Boolean accountLocked;
    
    private Boolean enabled;
    
    private LocalDateTime emailVerifiedAt;
    
    private LocalDateTime phoneVerifiedAt;
    
    private Integer totalBookings;
    
    private Integer totalReviews;
    
    private Integer helpfulReviews;
    
    private LocalDateTime createdDate;
    
    private LocalDateTime lastModifiedDate;
    
    private LocalDateTime lastLoginAt;
}
