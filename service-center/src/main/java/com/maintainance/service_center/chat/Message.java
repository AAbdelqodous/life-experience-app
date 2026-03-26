package com.maintainance.service_center.chat;

import com.maintainance.service_center.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "messages")
@EntityListeners(AuditingEntityListener.class)
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SenderType senderType; // CUSTOMER or CENTER_STAFF

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    private MessageType messageType = MessageType.TEXT;

    // For attachments
    private String mediaUrl;
    private String mediaType; // image/jpeg, application/pdf, etc.
    private Long mediaSize; // in bytes
    private String fileName;

    private Boolean isRead = false;
    private LocalDateTime readAt;

    private Boolean isDelivered = false;
    private LocalDateTime deliveredAt;

    private Boolean isSystemMessage = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_id")
    private Message replyTo; // For reply functionality

    // Edit history
    private Boolean isEdited = false;
    private LocalDateTime editedAt;
    private String originalContent;

    // Deletion
    private Boolean isDeleted = false;
    private LocalDateTime deletedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime updatedAt;
}
