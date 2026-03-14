package com.example.memory.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiChatTestRequest {

    private String systemPrompt;

    @NotBlank(message = "message must not be blank")
    private String message;
}
