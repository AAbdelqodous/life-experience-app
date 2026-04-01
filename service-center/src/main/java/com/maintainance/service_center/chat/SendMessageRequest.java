package com.maintainance.service_center.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    @NotNull(message = "Center ID is required")
    private Long centerId;

    @NotBlank(message = "Message content is required")
    @Size(max = 1000, message = "Message content must not exceed 1000 characters")
    private String content;
}
