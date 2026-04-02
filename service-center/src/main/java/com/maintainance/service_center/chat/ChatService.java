package com.maintainance.service_center.chat;

import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.common.PageResponse;
import com.maintainance.service_center.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MaintenanceCenterRepository centerRepository;

    public Conversation getOrCreateConversation(Long centerId, User user) {
        log.info("Getting or creating conversation for user {} and center {}", user.getId(), centerId);

        return conversationRepository.findByCustomerIdAndCenterId(user.getId(), centerId)
                .orElseGet(() -> {
                    MaintenanceCenter center = centerRepository.findById(centerId)
                            .orElseThrow(() -> new EntityNotFoundException("Center not found"));

                    Conversation conversation = Conversation.builder()
                            .customer(user)
                            .center(center)
                            .isActive(true)
                            .build();

                    Conversation savedConversation = conversationRepository.save(conversation);
                    log.info("Created new conversation with id {}", savedConversation.getId());
                    return savedConversation;
                });
    }

    @Transactional(readOnly = true)
    public PageResponse<ConversationResponse> getUserConversations(User user, int page, int size) {
        log.info("Fetching conversations for user {}, page {}, size {}", user.getId(), page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastMessageAt"));
        Page<Conversation> conversations = conversationRepository.findByCustomerIdOrderByLastMessageAtDesc(user.getId(), pageable);

        return PageResponse.of(conversations.map(this::mapToConversationResponse));
    }

    @Transactional
    public PageResponse<MessageResponse> getConversationMessages(Long conversationId, User user, int page, int size) {
        log.info("Fetching messages for conversation {} by user {}, page {}, size {}", conversationId, user.getId(), page, size);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation not found"));

        // Verify the conversation belongs to user
        if (!conversation.getCustomer().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You can only view your own conversations");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId, pageable);

        // Mark all CENTER_STAFF messages as read
        messageRepository.markAllAsReadInConversation(conversationId, SenderType.CENTER_STAFF);

        return PageResponse.of(messages.map(this::mapToMessageResponse));
    }

    @Transactional
    public MessageResponse sendMessage(SendMessageRequest request, User user) {
        log.info("Sending message from user {} to center {}", user.getId(), request.getCenterId());

        Conversation conversation = getOrCreateConversation(request.getCenterId(), user);

        Message message = Message.builder()
                .conversation(conversation)
                .sender(user)
                .senderType(SenderType.CUSTOMER)
                .content(request.getContent())
                .messageType(MessageType.TEXT)
                .isRead(false)
                .isDelivered(false)
                .isSystemMessage(false)
                .isEdited(false)
                .isDeleted(false)
                .build();

        Message savedMessage = messageRepository.save(message);

        // Update conversation's lastMessageAt
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        log.info("Message sent successfully with id {}", savedMessage.getId());

        return mapToMessageResponse(savedMessage);
    }

    @Transactional
    public MessageResponse sendMessageViaWebSocket(ChatMessageRequest request, User user) {
        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new EntityNotFoundException("Conversation not found"));

        if (!conversation.getCustomer().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Access denied to this conversation");
        }

        Message message = Message.builder()
                .content(request.getContent())
                .sender(user)
                .senderType(SenderType.CUSTOMER)
                .isRead(false)
                .conversation(conversation)
                .build();

        Message saved = messageRepository.save(message);

        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        return mapToMessageResponse(saved);
    }

    private ConversationResponse mapToConversationResponse(Conversation conversation) {
        String lastMessage = null;
        LocalDateTime lastMessageAt = conversation.getLastMessageAt();

        if (conversation.getMessages() != null && !conversation.getMessages().isEmpty()) {
            lastMessage = conversation.getMessages().get(0).getContent();
            if (lastMessageAt == null) {
                lastMessageAt = conversation.getMessages().get(0).getCreatedAt();
            }
        }

        Long unreadCount = messageRepository.countUnreadMessages(
                conversation.getId(),
                SenderType.CENTER_STAFF
        );

        return ConversationResponse.builder()
                .id(conversation.getId())
                .centerId(conversation.getCenter().getId())
                .centerNameAr(conversation.getCenter().getNameAr())
                .centerNameEn(conversation.getCenter().getNameEn())
                .lastMessage(lastMessage)
                .lastMessageAt(lastMessageAt)
                .unreadCount(unreadCount.intValue())
                .createdAt(conversation.getCreatedAt())
                .build();
    }

    private MessageResponse mapToMessageResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .content(message.getContent())
                .senderType(message.getSenderType())
                .read(message.getIsRead() != null ? message.getIsRead() : false)
                .createdAt(message.getCreatedAt())
                .build();
    }
}
