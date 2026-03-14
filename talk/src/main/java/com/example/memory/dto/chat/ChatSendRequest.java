package com.example.memory.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatSendRequest {

    @NotNull(message = "projectId must not be null")
    private Long projectId;

    @NotNull(message = "chatId must not be null")
    private Long chatId;

    @NotBlank(message = "message must not be blank")
    private String message;
}
