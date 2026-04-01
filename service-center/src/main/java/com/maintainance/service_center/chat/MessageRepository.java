package com.maintainance.service_center.chat;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId AND m.senderType = :senderType")
    List<Message> findByConversationAndSenderType(@Param("conversationId") Long conversationId, @Param("senderType") SenderType senderType);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :conversationId AND m.senderType = :senderType AND m.isRead = false")
    Long countUnreadMessages(@Param("conversationId") Long conversationId, @Param("senderType") SenderType senderType);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId AND m.isRead = false")
    List<Message> findUnreadMessages(@Param("conversationId") Long conversationId);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId AND m.senderType = 'CUSTOMER' AND m.isRead = false")
    List<Message> findUnreadCustomerMessages(@Param("conversationId") Long conversationId);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId AND m.senderType = 'CENTER_STAFF' AND m.isRead = false")
    List<Message> findUnreadCenterMessages(@Param("conversationId") Long conversationId);

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true, m.readAt = CURRENT_TIMESTAMP WHERE m.conversation.id = :convId AND m.senderType = :senderType")
    void markAllAsReadInConversation(@Param("convId") Long convId, @Param("senderType") SenderType senderType);
}
