package com.example.memory.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(VectorStoreProperties.class)
public class VectorStoreConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "app.vector.qdrant", name = "enabled", havingValue = "true")
    public QdrantGrpcClient qdrantGrpcClient(VectorStoreProperties properties) {
        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(
                        properties.getHost(),
                        properties.getGrpcPort(),
                        properties.isUseTls())
                .withTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));
        if (StringUtils.hasText(properties.getApiKey())) {
            builder.withApiKey(properties.getApiKey());
        }
        return builder.build();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "app.vector.qdrant", name = "enabled", havingValue = "true")
    public QdrantClient qdrantClient(QdrantGrpcClient qdrantGrpcClient) {
        return new QdrantClient(qdrantGrpcClient);
    }

    @Bean("memoryVectorStore")
    @Primary
    @ConditionalOnProperty(prefix = "app.vector.qdrant", name = "enabled", havingValue = "true")
    public VectorStore qdrantVectorStore(
            QdrantClient qdrantClient,
            EmbeddingModel embeddingModel,
            VectorStoreProperties properties) {
        return QdrantVectorStore.builder(qdrantClient, embeddingModel)
                .collectionName(properties.getCollectionName())
                .initializeSchema(properties.isInitializeSchema())
                .build();
    }

    @Bean("memoryVectorStore")
    @Primary
    @ConditionalOnProperty(prefix = "app.vector.qdrant", name = "enabled", havingValue = "false", matchIfMissing = true)
    public VectorStore noOpVectorStore() {
        return new NoOpVectorStore();
    }

    private static final class NoOpVectorStore implements VectorStore {

        @Override
        public void add(List<Document> documents) {
        }

        @Override
        public void delete(List<String> idList) {
        }

        @Override
        public void delete(Filter.Expression filterExpression) {
        }

        @Override
        public List<Document> similaritySearch(SearchRequest request) {
            return List.of();
        }

        @Override
        public <T> Optional<T> getNativeClient() {
            return Optional.empty();
        }
    }
}
