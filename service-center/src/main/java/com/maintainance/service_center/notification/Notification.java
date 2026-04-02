package com.maintainance.service_center.notification;

import com.maintainance.service_center.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "notifications")
@EntityListeners(AuditingEntityListener.class)
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User recipient;

    @Column(nullable = false)
    private String titleAr;

    @Column(nullable = false)
    private String titleEn;

    @Column(nullable = false, length = 500)
    private String bodyAr;

    @Column(nullable = false, length = 500)
    private String bodyEn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    private NotificationPriority notificationPriority = NotificationPriority.NORMAL;

    // Reference to the related entity
    private String referenceType; // BOOKING, REVIEW, MESSAGE, etc.
    private Long referenceId;

    // Additional data as key-value pairs
    @ElementCollection
    @CollectionTable(name = "notification_data")
    @MapKeyColumn(name = "data_key")
    @Column(name = "data_value")
    private Map<String, String> data = new HashMap<>();

    private Boolean isRead = false;
    private LocalDateTime readAt;

    private Boolean isPushSent = false;
    private LocalDateTime pushSentAt;

    private Boolean isEmailSent = false;
    private LocalDateTime emailSentAt;

    private Boolean isSmsSent = false;
    private LocalDateTime smsSentAt;

    private Boolean pushEnabled = true;
    private Boolean emailEnabled = true;
    private Boolean smsEnabled = true;

    // Firebase token for push notifications
    private String fcmToken;

    private Boolean isScheduled = false;
    private LocalDateTime scheduledAt;

    // Expiration
    private LocalDateTime expiresAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
