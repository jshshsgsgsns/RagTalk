package com.example.memory.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.example.memory.mapper.MemoryRecordMapper;
import com.example.memory.service.VectorMemoryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

class VectorStoreConfigurationTest {

    private final ApplicationContextRunner qdrantDisabledContextRunner = new ApplicationContextRunner()
            .withUserConfiguration(VectorStoreConfiguration.class)
            .withPropertyValues("app.vector.qdrant.enabled=false");

    private final ApplicationContextRunner stubVectorStoreContextRunner = new ApplicationContextRunner()
            .withUserConfiguration(StubVectorStoreWiringConfiguration.class);

    @Test
    void shouldProvideNoOpVectorStoreWhenQdrantIsDisabled() {
        qdrantDisabledContextRunner.run(context -> {
            assertThat(context).hasBean("memoryVectorStore");
            assertThat(context).doesNotHaveBean(io.qdrant.client.QdrantGrpcClient.class);
            assertThat(context).doesNotHaveBean(io.qdrant.client.QdrantClient.class);

            VectorStore vectorStore = context.getBean("memoryVectorStore", VectorStore.class);
            assertThat(vectorStore.similaritySearch(SearchRequest.builder()
                            .query("test query")
                            .topK(3)
                            .build()))
                    .isEmpty();
            assertThat(vectorStore.getNativeClient()).isEmpty();
        });
    }

    @Test
    void shouldInjectStubVectorStoreIntoVectorMemoryService() {
        stubVectorStoreContextRunner.run(context -> {
            assertThat(context).hasSingleBean(VectorMemoryService.class);
            assertThat(context).hasBean("memoryVectorStore");

            VectorMemoryService vectorMemoryService = context.getBean(VectorMemoryService.class);
            VectorStore injectedVectorStore =
                    (VectorStore) ReflectionTestUtils.getField(vectorMemoryService, "memoryVectorStore");

            assertThat(injectedVectorStore).isSameAs(context.getBean("memoryVectorStore"));
            assertThat(injectedVectorStore).isInstanceOf(TestStubVectorStore.class);
        });
    }

    @Configuration
    static class StubVectorStoreWiringConfiguration {

        @Bean
        MemoryRecordMapper memoryRecordMapper() {
            return mock(MemoryRecordMapper.class);
        }

        @Bean
        VectorStoreProperties vectorStoreProperties() {
            return new VectorStoreProperties();
        }

        @Bean("memoryVectorStore")
        VectorStore memoryVectorStore() {
            return new TestStubVectorStore();
        }

        @Bean
        VectorMemoryService vectorMemoryService(
                MemoryRecordMapper memoryRecordMapper,
                VectorStore memoryVectorStore,
                VectorStoreProperties vectorStoreProperties) {
            return new VectorMemoryService(memoryRecordMapper, memoryVectorStore, vectorStoreProperties);
        }
    }

    static final class TestStubVectorStore implements VectorStore {

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
    }
}
