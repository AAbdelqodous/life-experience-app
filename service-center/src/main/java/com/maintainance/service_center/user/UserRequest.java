package com.maintainance.service_center.user;

import com.maintainance.service_center.address.Address;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {
    
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstname;
    
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastname;
    
    private LocalDate dateOfBirth;
    
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;
    
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;
    
    @Size(max = 20, message = "Alternative phone must not exceed 20 characters")
    private String alternativePhone;
    
    @Size(max = 500, message = "Profile image URL must not exceed 500 characters")
    private String profileImageUrl;
    
    @Size(max = 1000, message = "Bio must not exceed 1000 characters")
    private String bio;
    
    private Address address;
    
    private Double lastKnownLatitude;
    
    private Double lastKnownLongitude;
    
    private Language preferredLanguage;
    
    private Boolean pushNotificationsEnabled;
    
    private Boolean emailNotificationsEnabled;
    
    private Boolean smsNotificationsEnabled;
    
    @Size(max = 500, message = "FCM token must not exceed 500 characters")
    private String fcmToken;
}
