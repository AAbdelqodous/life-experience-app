package com.maintainance.service_center.user;

import com.maintainance.service_center.address.Address;
import com.maintainance.service_center.booking.Booking;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.chat.Conversation;
import com.maintainance.service_center.complaint.Complaint;
import com.maintainance.service_center.favorite.UserFavorite;
import com.maintainance.service_center.notification.Notification;
import com.maintainance.service_center.review.Review;
import com.maintainance.service_center.role.Role;
import com.maintainance.service_center.search.SearchHistory;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "_user")
@EntityListeners(AuditingEntityListener.class)
public class User implements UserDetails, Principal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String firstname;
    private String lastname;
    private LocalDate dateOfBirth;

    @Column(unique = true)
    private String email;

    private String password;

    @Column(unique = true)
    private String phone;

    private String alternativePhone;

    // Profile
    private String profileImageUrl;
    private String bio;

    // Address
    @Embedded
    private Address address;

    // Location
    private Double lastKnownLatitude;
    private Double lastKnownLongitude;

    // Preferences
    @Enumerated(EnumType.STRING)
    private Language preferredLanguage = Language.AR;

    private Boolean pushNotificationsEnabled = true;
    private Boolean emailNotificationsEnabled = true;
    private Boolean smsNotificationsEnabled = false;

    // Firebase token for push notifications
    private String fcmToken;

    // Expo push token for push notifications
    @Column(name = "push_token")
    private String pushToken;

    // Account status
    private boolean accountLocked;
    private boolean enabled;
    private LocalDateTime emailVerifiedAt;
    private LocalDateTime phoneVerifiedAt;

    // User type - to distinguish between customers and center owners/staff
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private UserType userType = UserType.CUSTOMER;

    // Approval status - for center owners pending admin review
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status")
    private ApprovalStatus approvalStatus = ApprovalStatus.APPROVED;

    @ManyToMany(fetch = FetchType.EAGER)
    private List<Role> roles;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Token> tokens = new ArrayList<>();

    // Owned maintenance centers (if user is a center owner)
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    private List<MaintenanceCenter> ownedCenters = new ArrayList<>();

    // Customer relationships
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<Booking> bookings = new ArrayList<>();

    @OneToMany(mappedBy = "reviewer", cascade = CascadeType.ALL)
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<Conversation> conversations = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<UserFavorite> favorites = new ArrayList<>();

    @OneToMany(mappedBy = "recipient", cascade = CascadeType.ALL)
    private List<Notification> notifications = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<SearchHistory> searchHistory = new ArrayList<>();

    @OneToMany(mappedBy = "complainant", cascade = CascadeType.ALL)
    private List<Complaint> complaints = new ArrayList<>();

    // Statistics
    private Integer totalBookings = 0;
    private Integer totalReviews = 0;
    private Integer helpfulReviews = 0; // Reviews marked as helpful by others

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime lastModifiedDate;

    private LocalDateTime lastLoginAt;



    @Override
    public String getName() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.roles
                .stream()
                .map(r -> new SimpleGrantedAuthority(r.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !accountLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public String fullName(){
        return firstname + " " + lastname;
    }
}
