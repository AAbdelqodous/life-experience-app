package com.maintainance.service_center.notification;

import com.maintainance.service_center.user.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Get all notifications for current user (paginated)
     */
    public Page<NotificationResponse> getMyNotifications(Pageable pageable, User user) {
        Page<Notification> notifications = notificationRepository
                .findByRecipientOrderByCreatedAtDesc(user, pageable);
        return notifications.map(this::mapToResponse);
    }

    /**
     * Get unread notifications for current user
     */
    public Page<NotificationResponse> getUnreadNotifications(Pageable pageable, User user) {
        Page<Notification> notifications = notificationRepository
                .findByRecipientAndIsReadOrderByCreatedAtDesc(user, false, pageable);
        return notifications.map(this::mapToResponse);
    }

    /**
     * Get notification by ID
     */
    public NotificationResponse getNotificationById(Long id, User user) {
        Notification notification = notificationRepository
                .findByIdAndRecipient(id, user)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        return mapToResponse(notification);
    }

    /**
     * Get notifications by type
     */
    public List<NotificationResponse> getNotificationsByType(NotificationType type, User user) {
        List<Notification> notifications = notificationRepository
                .findByRecipientAndNotificationTypeOrderByCreatedAtDesc(user, type);
        return notifications.stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Get unread count
     */
    public UnreadCountResponse getUnreadCount(User user) {
        Long count = notificationRepository.countByRecipientAndIsRead(user, false);
        return UnreadCountResponse.builder()
                .unreadCount(count.intValue())
                .build();
    }

    /**
     * Create notification
     */
    @Transactional
    public NotificationResponse createNotification(NotificationRequest request, User user) {
        
        Notification notification = Notification.builder()
                .recipient(user)
                .titleAr(request.getTitleAr())
                .titleEn(request.getTitleEn())
                .bodyAr(request.getBodyAr())
                .bodyEn(request.getBodyEn())
                .notificationType(request.getNotificationType())
                .notificationPriority(request.getNotificationPriority() != null ? 
                        request.getNotificationPriority() : NotificationPriority.NORMAL)
                .referenceType(request.getReferenceType())
                .referenceId(request.getReferenceId())
                .data(request.getData())
                .isRead(false)
                .isPushSent(false)
                .isEmailSent(false)
                .isSmsSent(false)
                .pushEnabled(user.getPushNotificationsEnabled())
                .emailEnabled(user.getEmailNotificationsEnabled())
                .smsEnabled(user.getSmsNotificationsEnabled())
                .fcmToken(user.getFcmToken())
                .isScheduled(request.getScheduledAt() != null)
                .scheduledAt(request.getScheduledAt())
                .expiresAt(request.getExpiresAt())
                .build();
        
        Notification savedNotification = notificationRepository.save(notification);
        log.info("Notification created for user ID: {}", user.getId());
        
        return mapToResponse(savedNotification);
    }

    /**
     * Mark notification as read
     */
    @Transactional
    public NotificationResponse markAsRead(Long id, User user) {
        Notification notification = notificationRepository
                .findByIdAndRecipient(id, user)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            Notification saved = notificationRepository.save(notification);
            log.info("Notification ID {} marked as read for user ID: {}", id, user.getId());
            return mapToResponse(saved);
        }
        
        return mapToResponse(notification);
    }

    /**
     * Mark all notifications as read
     */
    @Transactional
    public void markAllAsRead(User user) {
        List<Notification> unreadNotifications = notificationRepository
                .findByRecipientAndIsReadFalse(user);
        
        for (Notification notification : unreadNotifications) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
        }
        
        notificationRepository.saveAll(unreadNotifications);
        log.info("All notifications marked as read for user ID: {}", user.getId());
    }

    /**
     * Delete notification
     */
    @Transactional
    public void deleteNotification(Long id, User user) {
        Notification notification = notificationRepository
                .findByIdAndRecipient(id, user)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        notificationRepository.delete(notification);
        log.info("Notification ID {} deleted for user ID: {}", id, user.getId());
    }

    /**
     * Delete all read notifications
     */
    @Transactional
    public void deleteReadNotifications(User user) {
        List<Notification> readNotifications = notificationRepository
                .findByRecipientAndIsReadOrderByCreatedAtDesc(user, true, Pageable.unpaged())
                .getContent();
        
        notificationRepository.deleteAll(readNotifications);
        log.info("All read notifications deleted for user ID: {}", user.getId());
    }

    /**
     * Map Notification entity to NotificationResponse DTO
     */
    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .titleAr(notification.getTitleAr())
                .titleEn(notification.getTitleEn())
                .bodyAr(notification.getBodyAr())
                .bodyEn(notification.getBodyEn())
                .notificationType(notification.getNotificationType())
                .notificationPriority(notification.getNotificationPriority())
                .referenceType(notification.getReferenceType())
                .referenceId(notification.getReferenceId())
                .data(notification.getData())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .isPushSent(notification.getIsPushSent())
                .pushSentAt(notification.getPushSentAt())
                .isEmailSent(notification.getIsEmailSent())
                .emailSentAt(notification.getEmailSentAt())
                .isSmsSent(notification.getIsSmsSent())
                .smsSentAt(notification.getSmsSentAt())
                .isScheduled(notification.getIsScheduled())
                .scheduledAt(notification.getScheduledAt())
                .expiresAt(notification.getExpiresAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
