package com.maintainance.service_center.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessageRequest {
    @NotBlank(message = "Message content is required")
    @Size(max = 1000, message = "Message content must not exceed 1000 characters")
    private String content;

    private String messageType;
}
