package com.maintainance.service_center.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class NotificationRequest {
    
    @NotBlank(message = "Title (Arabic) is required")
    private String titleAr;
    
    @NotBlank(message = "Title (English) is required")
    private String titleEn;
    
    @NotBlank(message = "Body (Arabic) is required")
    private String bodyAr;
    
    @NotBlank(message = "Body (English) is required")
    private String bodyEn;
    
    @NotNull(message = "Notification type is required")
    private NotificationType notificationType;
    
    private NotificationPriority notificationPriority;
    
    private String referenceType;
    
    private Long referenceId;
    
    private Map<String, String> data;
    
    private LocalDateTime scheduledAt;
    
    private LocalDateTime expiresAt;
}
