package com.maintainance.service_center.chat;

import com.maintainance.service_center.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Client sends to: /app/chat.send
     * Message is broadcast to: /topic/conversations/{conversationId}
     * and also to the user's personal queue: /user/{username}/queue/messages
     */
    @MessageMapping("/chat.send")
    public void sendMessage(
            @Payload ChatMessageRequest request,
            Principal principal) {

        if (principal == null) {
            log.warn("Unauthenticated WebSocket message attempt");
            return;
        }

        User user = (User) ((org.springframework.security.core.Authentication) principal).getPrincipal();

        MessageResponse response = chatService.sendMessageViaWebSocket(request, user);

        // Broadcast to conversation topic (both user and center can subscribe)
        messagingTemplate.convertAndSend(
                "/topic/conversations/" + request.getConversationId(),
                response
        );

        // Also notify the user's personal queue (for multi-device support)
        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/messages",
                response
        );
    }
}
