package com.example.memory.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@Valid
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    @Valid
    private ModelProperties chat = new ModelProperties();

    @Valid
    private ModelProperties embedding = new ModelProperties();

    @Valid
    private Providers providers = new Providers();

    @Data
    public static class ModelProperties {

        @NotNull
        private ProviderType provider = ProviderType.OLLAMA;

        @NotBlank
        private String model;

        private String baseUrl;

        private String apiKey;

        private String completionsPath;

        private String embeddingsPath;

        private Double temperature;

        private Integer maxTokens;

        private Integer dimensions;
    }

    @Data
    public static class Providers {

        @Valid
        private ProviderProperties openai = ProviderProperties.openAiDefaults();

        @Valid
        private ProviderProperties ollama = ProviderProperties.ollamaDefaults();
    }

    @Data
    public static class ProviderProperties {

        @NotBlank
        private String baseUrl;

        private String apiKey;

        @NotBlank
        private String completionsPath;

        @NotBlank
        private String embeddingsPath;

        public static ProviderProperties openAiDefaults() {
            ProviderProperties properties = new ProviderProperties();
            properties.setBaseUrl("https://api.openai.com");
            properties.setCompletionsPath("/v1/chat/completions");
            properties.setEmbeddingsPath("/v1/embeddings");
            return properties;
        }

        public static ProviderProperties ollamaDefaults() {
            ProviderProperties properties = new ProviderProperties();
            properties.setBaseUrl("http://localhost:11434");
            properties.setCompletionsPath("/v1/chat/completions");
            properties.setEmbeddingsPath("/v1/embeddings");
            properties.setApiKey("ollama");
            return properties;
        }
    }

    public enum ProviderType {
        OPENAI,
        OLLAMA
    }
}
