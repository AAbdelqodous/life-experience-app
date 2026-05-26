package com.maintainance.service_center.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private Long id;
    private Long conversationId;
    private Integer senderId;
    private SenderType senderType;
    private String senderName;
    private String content;
    private boolean read;
    private LocalDateTime createdAt;
}
