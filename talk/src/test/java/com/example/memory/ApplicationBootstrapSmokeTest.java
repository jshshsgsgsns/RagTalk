package com.example.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:target/application-bootstrap-smoke-test.db",
        "app.vector.qdrant.enabled=false",
        "app.ai.chat.provider=ollama",
        "app.ai.chat.model=bootstrap-chat-model",
        "app.ai.embedding.provider=ollama",
        "app.ai.embedding.model=bootstrap-embedding-model"
})
@AutoConfigureMockMvc
class ApplicationBootstrapSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VectorStore memoryVectorStore;

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private ObjectProvider<io.qdrant.client.QdrantGrpcClient> qdrantGrpcClientProvider;

    @Autowired
    private ObjectProvider<io.qdrant.client.QdrantClient> qdrantClientProvider;

    @Test
    void applicationShouldStartWithQdrantDisabledAndExposeHealthEndpoint() throws Exception {
        assertThat(memoryVectorStore).isNotNull();
        assertThat(chatModel).isNotNull();
        assertThat(chatClient).isNotNull();
        assertThat(embeddingModel).isNotNull();
        assertThat(qdrantGrpcClientProvider.getIfAvailable()).isNull();
        assertThat(qdrantClientProvider.getIfAvailable()).isNull();
        assertThat(memoryVectorStore.getNativeClient()).isEmpty();

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.application").value("memory-backend"));
    }
}
