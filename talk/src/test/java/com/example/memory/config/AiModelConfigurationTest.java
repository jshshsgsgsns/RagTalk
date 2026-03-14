package com.example.memory.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AiModelConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AiModelConfiguration.class);

    @Test
    void shouldWireChatModelAndChatClientWhenChatProviderIsConfigured() {
        contextRunner
                .withPropertyValues(
                        "app.ai.chat.provider=openai",
                        "app.ai.chat.model=test-chat-model",
                        "app.ai.chat.temperature=0.3",
                        "app.ai.chat.max-tokens=256",
                        "app.ai.embedding.provider=ollama",
                        "app.ai.embedding.model=test-embedding-model",
                        "app.ai.providers.openai.base-url=https://openai.example.test",
                        "app.ai.providers.openai.api-key=test-openai-key",
                        "app.ai.providers.openai.completions-path=/v1/chat/completions",
                        "app.ai.providers.openai.embeddings-path=/v1/embeddings",
                        "app.ai.providers.ollama.base-url=http://localhost:11434",
                        "app.ai.providers.ollama.api-key=ollama",
                        "app.ai.providers.ollama.completions-path=/v1/chat/completions",
                        "app.ai.providers.ollama.embeddings-path=/v1/embeddings")
                .run(context -> {
                    assertThat(context).hasBean("chatModel");
                    assertThat(context).hasBean("chatClient");

                    AiProperties aiProperties = context.getBean(AiProperties.class);
                    assertThat(aiProperties.getChat().getProvider()).isEqualTo(AiProperties.ProviderType.OPENAI);
                    assertThat(aiProperties.getChat().getModel()).isEqualTo("test-chat-model");

                    assertThat(context.getBean("chatModel")).isInstanceOf(ChatModel.class);
                    assertThat(context.getBean("chatClient")).isInstanceOf(ChatClient.class);
                });
    }

    @Test
    void shouldWireEmbeddingModelWhenEmbeddingProviderIsConfigured() {
        contextRunner
                .withPropertyValues(
                        "app.ai.chat.provider=ollama",
                        "app.ai.chat.model=test-chat-model",
                        "app.ai.embedding.provider=openai",
                        "app.ai.embedding.model=test-embedding-model",
                        "app.ai.embedding.dimensions=128",
                        "app.ai.providers.openai.base-url=https://openai.example.test",
                        "app.ai.providers.openai.api-key=test-openai-key",
                        "app.ai.providers.openai.completions-path=/v1/chat/completions",
                        "app.ai.providers.openai.embeddings-path=/v1/embeddings",
                        "app.ai.providers.ollama.base-url=http://localhost:11434",
                        "app.ai.providers.ollama.api-key=ollama",
                        "app.ai.providers.ollama.completions-path=/v1/chat/completions",
                        "app.ai.providers.ollama.embeddings-path=/v1/embeddings")
                .run(context -> {
                    assertThat(context).hasBean("embeddingModel");

                    AiProperties aiProperties = context.getBean(AiProperties.class);
                    assertThat(aiProperties.getEmbedding().getProvider()).isEqualTo(AiProperties.ProviderType.OPENAI);
                    assertThat(aiProperties.getEmbedding().getModel()).isEqualTo("test-embedding-model");
                    assertThat(aiProperties.getEmbedding().getDimensions()).isEqualTo(128);

                    assertThat(context.getBean("embeddingModel")).isInstanceOf(EmbeddingModel.class);
                });
    }
}
