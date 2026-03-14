package com.example.memory.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.document.MetadataMode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties({AiProperties.class, ChatFlowProperties.class, MemoryExtractionProperties.class, RagChatProperties.class})
public class AiModelConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @ConditionalOnMissingBean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean("chatModel")
    public ChatModel chatModel(
            AiProperties aiProperties,
            RestClient.Builder restClientBuilder,
            WebClient.Builder webClientBuilder) {
        AiProperties.ModelProperties chatProperties = aiProperties.getChat();
        ResolvedProvider provider = resolveProvider(chatProperties, aiProperties);

        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(chatProperties.getModel());
        if (chatProperties.getTemperature() != null) {
            optionsBuilder.temperature(chatProperties.getTemperature());
        }
        if (chatProperties.getMaxTokens() != null) {
            optionsBuilder.maxTokens(chatProperties.getMaxTokens());
        }

        return OpenAiChatModel.builder()
                .openAiApi(buildOpenAiApi(provider, restClientBuilder, webClientBuilder))
                .defaultOptions(optionsBuilder.build())
                .build();
    }

    @Bean("chatClient")
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.create(chatModel);
    }

    @Bean("embeddingModel")
    public EmbeddingModel embeddingModel(
            AiProperties aiProperties,
            RestClient.Builder restClientBuilder,
            WebClient.Builder webClientBuilder) {
        AiProperties.ModelProperties embeddingProperties = aiProperties.getEmbedding();
        ResolvedProvider provider = resolveProvider(embeddingProperties, aiProperties);

        OpenAiEmbeddingOptions.Builder optionsBuilder = OpenAiEmbeddingOptions.builder()
                .model(embeddingProperties.getModel());
        if (embeddingProperties.getDimensions() != null) {
            optionsBuilder.dimensions(embeddingProperties.getDimensions());
        }

        return new OpenAiEmbeddingModel(
                buildOpenAiApi(provider, restClientBuilder, webClientBuilder),
                MetadataMode.NONE,
                optionsBuilder.build());
    }

    private OpenAiApi buildOpenAiApi(
            ResolvedProvider provider,
            RestClient.Builder restClientBuilder,
            WebClient.Builder webClientBuilder) {
        return OpenAiApi.builder()
                .baseUrl(provider.baseUrl())
                .apiKey(provider.apiKey())
                .completionsPath(provider.completionsPath())
                .embeddingsPath(provider.embeddingsPath())
                .restClientBuilder(restClientBuilder)
                .webClientBuilder(webClientBuilder)
                .build();
    }

    private ResolvedProvider resolveProvider(
            AiProperties.ModelProperties modelProperties,
            AiProperties aiProperties) {
        AiProperties.ProviderProperties providerDefaults = switch (modelProperties.getProvider()) {
            case OPENAI -> aiProperties.getProviders().getOpenai();
            case OLLAMA -> aiProperties.getProviders().getOllama();
        };

        return new ResolvedProvider(
                valueOrDefault(modelProperties.getBaseUrl(), providerDefaults.getBaseUrl()),
                valueOrDefault(modelProperties.getApiKey(), providerDefaults.getApiKey(), "test-key"),
                valueOrDefault(modelProperties.getCompletionsPath(), providerDefaults.getCompletionsPath()),
                valueOrDefault(modelProperties.getEmbeddingsPath(), providerDefaults.getEmbeddingsPath()));
    }

    private String valueOrDefault(String preferredValue, String defaultValue) {
        return valueOrDefault(preferredValue, defaultValue, null);
    }

    private String valueOrDefault(String preferredValue, String defaultValue, String fallbackValue) {
        if (StringUtils.hasText(preferredValue)) {
            return preferredValue;
        }
        if (StringUtils.hasText(defaultValue)) {
            return defaultValue;
        }
        return fallbackValue;
    }

    private record ResolvedProvider(
            String baseUrl,
            String apiKey,
            String completionsPath,
            String embeddingsPath) {
    }
}
