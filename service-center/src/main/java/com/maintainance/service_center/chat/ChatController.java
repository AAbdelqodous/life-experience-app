package com.maintainance.service_center.chat;

import com.maintainance.service_center.common.PageResponse;
import com.maintainance.service_center.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("conversations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/center")
    @Operation(summary = "Get conversations for the authenticated owner's center")
    public ResponseEntity<PageResponse<ConversationResponse>> getCenterConversations(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(chatService.getCenterConversations(user, page, size));
    }

    @GetMapping
    @Operation(summary = "Get current user's conversations")
    public ResponseEntity<PageResponse<ConversationResponse>> getUserConversations(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(chatService.getUserConversations(user, page, size));
    }

    @GetMapping("/center/messages")
    @Operation(summary = "Get messages for a conversation (center owner view)")
    public ResponseEntity<PageResponse<MessageResponse>> getCenterConversationMessages(
            @RequestParam Long conversationId,
            @AuthenticationPrincipal User owner,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(chatService.getCenterConversationMessages(conversationId, owner, page, size));
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "Get messages for a specific conversation")
    public ResponseEntity<PageResponse<MessageResponse>> getConversationMessages(
            @PathVariable Long id,
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(chatService.getConversationMessages(id, user, page, size));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark conversation as read")
    public ResponseEntity<Void> markConversationAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        chatService.markConversationAsRead(id, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/messages")
    @Operation(summary = "Send a message to a conversation", description = "Send a message within an existing conversation. Works for both customers and center staff.")
    public ResponseEntity<MessageResponse> sendMessageToConversation(
            @PathVariable Long id,
            @RequestBody @Valid ConversationMessageRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(chatService.sendMessageToConversation(id, request.getContent(), user));
    }

    @PostMapping("/messages")
    @Operation(summary = "Send a message to a center")
    public ResponseEntity<MessageResponse> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(chatService.sendMessage(request, user));
    }
}
