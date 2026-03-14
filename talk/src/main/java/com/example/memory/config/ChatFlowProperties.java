package com.example.memory.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.chat")
public class ChatFlowProperties {

    @Min(1)
    private int contextMessageLimit = 12;

    private String defaultSystemPrompt = "You are a helpful assistant.";
}
