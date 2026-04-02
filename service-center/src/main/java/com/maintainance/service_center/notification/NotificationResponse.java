package com.maintainance.service_center.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    
    private Long id;
    
    private String titleAr;
    
    private String titleEn;
    
    private String bodyAr;
    
    private String bodyEn;
    
    private NotificationType notificationType;
    
    private NotificationPriority notificationPriority;
    
    private String referenceType;
    
    private Long referenceId;
    
    private Map<String, String> data;
    
    private Boolean isRead;
    
    private LocalDateTime readAt;
    
    private Boolean isPushSent;
    
    private LocalDateTime pushSentAt;
    
    private Boolean isEmailSent;
    
    private LocalDateTime emailSentAt;
    
    private Boolean isSmsSent;
    
    private LocalDateTime smsSentAt;
    
    private Boolean isScheduled;
    
    private LocalDateTime scheduledAt;
    
    private LocalDateTime expiresAt;
    
    private LocalDateTime createdAt;
}
