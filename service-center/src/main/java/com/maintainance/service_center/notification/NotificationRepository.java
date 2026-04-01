package com.maintainance.service_center.notification;

import com.maintainance.service_center.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient);
    
    Page<Notification> findByRecipientOrderByCreatedAtDesc(User recipient, Pageable pageable);
    
    Page<Notification> findByRecipientAndIsReadOrderByCreatedAtDesc(User recipient, Boolean isRead, Pageable pageable);
    
    Optional<Notification> findByIdAndRecipient(Long id, User recipient);
    
    Long countByRecipientAndIsRead(User recipient, Boolean isRead);
    
    List<Notification> findByRecipientAndIsReadFalse(User recipient);
    
    List<Notification> findByRecipientAndNotificationTypeOrderByCreatedAtDesc(User recipient, NotificationType notificationType);
    
    List<Notification> findByRecipientAndReferenceTypeAndReferenceIdOrderByCreatedAtDesc(
            User recipient, String referenceType, Long referenceId);
    
    List<Notification> findByIsScheduledTrueAndScheduledAtBefore(LocalDateTime now);
    
    List<Notification> findByIsPushSentFalseAndPushEnabledTrueAndRecipient_PushNotificationsEnabledTrue();
    
    List<Notification> findByIsEmailSentFalseAndEmailEnabledTrueAndRecipient_EmailNotificationsEnabledTrue();
    
    List<Notification> findByIsSmsSentFalseAndSmsEnabledTrueAndRecipient_SmsNotificationsEnabledTrue();
}
