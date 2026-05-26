package com.maintainance.service_center.chat;

import com.maintainance.service_center.center.CenterResolverService;
import com.maintainance.service_center.center.MaintenanceCenter;
import com.maintainance.service_center.center.MaintenanceCenterRepository;
import com.maintainance.service_center.common.PageResponse;
import com.maintainance.service_center.staff.CenterPermission;
import com.maintainance.service_center.staff.CenterSecurityService;
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
    private final CenterSecurityService centerSecurity;
    private final CenterResolverService centerResolver;

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

        // Verify the conversation belongs to the user (as customer) or to their center (as staff with MANAGE_CHAT)
        boolean isCustomer = conversation.getCustomer().getId().equals(user.getId());
        boolean hasChatAccess = centerSecurity.hasPermission(
                conversation.getCenter().getId(), user, CenterPermission.MANAGE_CHAT);
        if (!isCustomer && !hasChatAccess) {
            throw new IllegalArgumentException("Access denied to this conversation");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId, pageable);

        // Mark all STAFF messages as read
        messageRepository.markAllAsReadInConversation(conversationId, SenderType.STAFF);

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

    @Transactional(readOnly = true)
    public PageResponse<ConversationResponse> getCenterConversations(User caller, int page, int size) {
        log.info("Fetching center conversations for user {}, page {}, size {}", caller.getId(), page, size);

        MaintenanceCenter center = centerResolver.resolveCenter(caller);
        centerSecurity.requirePermission(center.getId(), caller, CenterPermission.MANAGE_CHAT);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastMessageAt"));
        Page<Conversation> conversations = conversationRepository.findActiveConversationsByCenter(center.getId(), pageable);

        return PageResponse.of(conversations.map(this::mapToCenterConversationResponse));
    }

    @Transactional
    public PageResponse<MessageResponse> getCenterConversationMessages(Long conversationId, User caller, int page, int size) {
        log.info("Fetching messages for conversation {} by user {}", conversationId, caller.getId());

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation not found"));

        // Permission check: MANAGE_CHAT required (Owner, Branch Manager, Receptionist)
        centerSecurity.requirePermission(
                conversation.getCenter().getId(), caller, CenterPermission.MANAGE_CHAT);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId, pageable);

        messageRepository.markAllAsReadInConversation(conversationId, SenderType.CUSTOMER);

        return PageResponse.of(messages.map(this::mapToMessageResponse));
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

    private ConversationResponse mapToCenterConversationResponse(Conversation conversation) {
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
                SenderType.CUSTOMER
        );

        return ConversationResponse.builder()
                .id(conversation.getId())
                .centerId(conversation.getCenter().getId())
                .centerNameAr(conversation.getCenter().getNameAr())
                .centerNameEn(conversation.getCenter().getNameEn())
                .customerId(conversation.getCustomer().getId())
                .customerName(conversation.getCustomer().fullName())
                .lastMessage(lastMessage)
                .lastMessageAt(lastMessageAt)
                .unreadCount(unreadCount.intValue())
                .createdAt(conversation.getCreatedAt())
                .build();
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
                SenderType.STAFF
        );

        User customer = conversation.getCustomer();
        String customerName = (customer.getFirstname() != null ? customer.getFirstname() : "") +
                " " + (customer.getLastname() != null ? customer.getLastname() : "");

        return ConversationResponse.builder()
                .id(conversation.getId())
                .centerId(conversation.getCenter().getId())
                .centerNameAr(conversation.getCenter().getNameAr())
                .centerNameEn(conversation.getCenter().getNameEn())
                .customerId(customer.getId())
                .customerName(customerName.trim())
                .lastMessage(lastMessage)
                .lastMessageAt(lastMessageAt)
                .unreadCount(unreadCount.intValue())
                .createdAt(conversation.getCreatedAt())
                .build();
    }

    @Transactional
    public void markConversationAsRead(Long conversationId, User caller) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation not found"));

        boolean isCustomer = caller.getId().equals(conversation.getCustomer().getId());

        if (isCustomer) {
            // Customer marks center messages as read
            messageRepository.markAllAsReadInConversation(conversationId, SenderType.STAFF);
            conversation.setUnreadCenterMessages(0);
        } else {
            // Center staff marks customer messages as read
            centerSecurity.requirePermission(
                    conversation.getCenter().getId(), caller, CenterPermission.MANAGE_CHAT);
            messageRepository.markAllAsReadInConversation(conversationId, SenderType.CUSTOMER);
            conversation.setUnreadCustomerMessages(0);
        }
        conversationRepository.save(conversation);
    }

    @Transactional
    public MessageResponse sendMessageToConversation(Long conversationId, String content, User caller) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation not found"));

        boolean isCustomer = caller.getId().equals(conversation.getCustomer().getId());
        SenderType senderType;

        if (isCustomer) {
            senderType = SenderType.CUSTOMER;
        } else {
            // Verify center staff has MANAGE_CHAT permission
            centerSecurity.requirePermission(
                    conversation.getCenter().getId(), caller, CenterPermission.MANAGE_CHAT);
            senderType = SenderType.STAFF;
        }

        Message message = Message.builder()
                .conversation(conversation)
                .sender(caller)
                .senderType(senderType)
                .content(content)
                .messageType(MessageType.TEXT)
                .isRead(false)
                .isDelivered(false)
                .isSystemMessage(false)
                .isEdited(false)
                .isDeleted(false)
                .build();

        Message saved = messageRepository.save(message);

        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        return mapToMessageResponse(saved);
    }

    private MessageResponse mapToMessageResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversation() != null ? message.getConversation().getId() : null)
                .senderId(message.getSender() != null ? message.getSender().getId() : null)
                .senderType(message.getSenderType())
                .senderName(message.getSender() != null ? message.getSender().fullName() : null)
                .content(message.getContent())
                .read(message.getIsRead() != null ? message.getIsRead() : false)
                .createdAt(message.getCreatedAt())
                .build();
    }
}
