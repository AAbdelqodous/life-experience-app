package com.maintainance.service_center.notification;

import com.maintainance.service_center.user.User;
import jakarta.persistence.EntityNotFoundException;
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
                .orElseThrow(() -> new EntityNotFoundException("Notification not found"));
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
     * Get notification stats (total + unread)
     */
    public NotificationStatsResponse getStats(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must be authenticated");
        }
        long total = notificationRepository.countByRecipient(user);
        long unread = notificationRepository.countByRecipientAndIsRead(user, false);
        return NotificationStatsResponse.builder()
                .totalCount(total)
                .unreadCount(unread)
                .build();
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
     * Spec 022 FR-DR-027/028: enqueue a bilingual generic "your booking moved" notification
     * for the customer. Per FR-DR-028 the message MUST NOT reveal internal dept names.
     * When {@code hadActiveQuote} is true, FR-DR-029 also asks the customer to review the
     * upcoming revised quote.
     * <p>This method is intended to be called AFTER the re-route transaction has committed
     * (see {@code RerouteService}); the resulting notification row is persisted in its own
     * transaction so a re-route rollback does not leave behind a phantom notification.
     */
    @Transactional
    public void notifyBookingRerouted(User customer, Long bookingId, boolean hadActiveQuote) {
        String titleEn = "Booking updated";
        String titleAr = "تم تحديث الحجز";
        String bodyEn = "Our team has updated your booking.";
        String bodyAr = "قام فريقنا بتحديث حجزك.";
        if (hadActiveQuote) {
            bodyEn += " A revised quote will be sent to you. Please review it before work continues.";
            bodyAr += " سيتم إرسال عرض سعر مُحدث. يُرجى مراجعته قبل استكمال العمل.";
        }
        NotificationRequest request = NotificationRequest.builder()
                .titleAr(titleAr).titleEn(titleEn)
                .bodyAr(bodyAr).bodyEn(bodyEn)
                .notificationType(NotificationType.BOOKING_REROUTED)
                .notificationPriority(NotificationPriority.NORMAL)
                .referenceType("BOOKING")
                .referenceId(bookingId)
                .build();
        createNotification(request, customer);
    }

    // ── Quote requests / marketplace (spec 009/024) ──────────────────────────

    /** → matched center owner: a new quote request (lead) arrived in their category. */
    @Transactional
    public void notifyNewQuoteRequest(User centerUser, Long requestId, String categoryEn, String categoryAr) {
        if (centerUser == null) return;
        createNotification(NotificationRequest.builder()
                .titleEn("New quote request").titleAr("طلب عرض سعر جديد")
                .bodyEn("A customer requested a quote for " + categoryEn + ".")
                .bodyAr("طلب عميل عرض سعر لـ " + categoryAr + ".")
                .notificationType(NotificationType.NEW_QUOTE_REQUEST)
                .notificationPriority(NotificationPriority.NORMAL)
                .referenceType("QUOTE_REQUEST").referenceId(requestId)
                .build(), centerUser);
    }

    /** → customer: a center submitted a quote on their request. */
    @Transactional
    public void notifyQuoteReceived(User customer, Long requestId) {
        if (customer == null) return;
        createNotification(NotificationRequest.builder()
                .titleEn("New quote received").titleAr("وصل عرض سعر جديد")
                .bodyEn("A center sent you a quote. Compare and choose the best one.")
                .bodyAr("أرسل لك أحد المراكز عرض سعر. قارن واختر الأفضل.")
                .notificationType(NotificationType.QUOTE_RECEIVED)
                .notificationPriority(NotificationPriority.NORMAL)
                .referenceType("QUOTE_REQUEST").referenceId(requestId)
                .build(), customer);
    }

    /** → winning center owner: the customer accepted their quote (a booking was created). */
    @Transactional
    public void notifyQuoteWon(User centerUser, Long bookingId) {
        if (centerUser == null) return;
        createNotification(NotificationRequest.builder()
                .titleEn("You won the job!").titleAr("لقد فزت بالمهمة!")
                .bodyEn("A customer accepted your quote. A new booking has been created.")
                .bodyAr("قبل العميل عرضك. تم إنشاء حجز جديد.")
                .notificationType(NotificationType.QUOTE_ACCEPTED)
                .notificationPriority(NotificationPriority.HIGH)
                .referenceType("BOOKING").referenceId(bookingId)
                .build(), centerUser);
    }

    /** → other center owners: the customer chose a different center. */
    @Transactional
    public void notifyQuoteNotSelected(User centerUser, Long requestId) {
        if (centerUser == null) return;
        createNotification(NotificationRequest.builder()
                .titleEn("Quote not selected").titleAr("لم يتم اختيار العرض")
                .bodyEn("The customer chose another center for this request.")
                .bodyAr("اختار العميل مركزاً آخر لهذا الطلب.")
                .notificationType(NotificationType.QUOTE_NOT_SELECTED)
                .notificationPriority(NotificationPriority.LOW)
                .referenceType("QUOTE_REQUEST").referenceId(requestId)
                .build(), centerUser);
    }

    /**
     * Mark notification as read
     */
    @Transactional
    public NotificationResponse markAsRead(Long id, User user) {
        Notification notification = notificationRepository
                .findByIdAndRecipient(id, user)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found"));
        
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
                .orElseThrow(() -> new EntityNotFoundException("Notification not found"));
        
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
