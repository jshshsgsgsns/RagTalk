package com.example.memory.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiEmbeddingTestRequest {

    @NotBlank(message = "text must not be blank")
    private String text;
}
