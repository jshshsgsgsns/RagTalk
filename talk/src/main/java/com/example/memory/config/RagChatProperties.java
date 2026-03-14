package com.example.memory.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.rag")
public class RagChatProperties {

    @Min(1)
    private int memoryTopK = 6;

    @Min(1)
    private int historyMessageLimit = 10;

    private String systemPrompt = "You are a helpful assistant. Use retrieved memories when relevant, but do not invent facts.";
}
