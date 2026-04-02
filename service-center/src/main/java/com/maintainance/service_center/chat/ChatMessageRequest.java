package com.maintainance.service_center.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessageRequest {
    @NotNull
    private Long conversationId;
    @NotBlank
    @Size(max = 1000)
    private String content;
}
