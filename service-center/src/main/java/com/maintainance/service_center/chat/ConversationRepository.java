package com.maintainance.service_center.chat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Page<Conversation> findByCustomerIdOrderByLastMessageAtDesc(Integer customerId, Pageable pageable);

    Optional<Conversation> findByCustomerIdAndCenterId(Integer customerId, Long centerId);

    @Query("SELECT c FROM Conversation c WHERE c.customer.id = :customerId AND c.isActive = true")
    Page<Conversation> findActiveConversations(@Param("customerId") Integer customerId, Pageable pageable);

    @Query("SELECT c FROM Conversation c WHERE c.center.id = :centerId AND c.isActive = true")
    Page<Conversation> findActiveConversationsByCenter(@Param("centerId") Long centerId, Pageable pageable);

    @Query("SELECT c FROM Conversation c WHERE c.customer.id = :customerId AND c.center.id = :centerId")
    Optional<Conversation> findByCustomerAndCenter(@Param("customerId") Integer customerId, @Param("centerId") Long centerId);
}
