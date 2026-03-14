package com.example.memory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.memory.config.VectorStoreProperties;
import com.example.memory.entity.MemoryRecord;
import com.example.memory.mapper.MemoryRecordMapper;
import com.example.memory.vo.vector.RagMemorySearchResult;
import com.example.memory.vo.vector.VectorMemorySearchResponseVO;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.converter.PrintFilterExpressionConverter;

@ExtendWith(MockitoExtension.class)
class VectorMemoryServiceTest {

    @Mock
    private MemoryRecordMapper memoryRecordMapper;

    @Mock
    private VectorStore memoryVectorStore;

    @Captor
    private ArgumentCaptor<List<Document>> documentsCaptor;

    @Captor
    private ArgumentCaptor<SearchRequest> searchRequestCaptor;

    private VectorMemoryService vectorMemoryService;

    @BeforeEach
    void setUp() {
        VectorStoreProperties properties = new VectorStoreProperties();
        properties.setSearchTopKDefault(3);
        vectorMemoryService = new VectorMemoryService(memoryRecordMapper, memoryVectorStore, properties);
    }

    @Test
    void upsertMemoryShouldConvertHighValueMemoryToDocumentAndCallVectorStore() {
        MemoryRecord record = buildMemoryRecord(
                101L,
                7L,
                70L,
                700L,
                "project",
                "decision",
                "important decision",
                "use cards layout",
                "detail");
        given(memoryRecordMapper.selectById(101L)).willReturn(record);

        var response = vectorMemoryService.upsertMemory(101L);

        verify(memoryVectorStore).add(documentsCaptor.capture());
        Document document = documentsCaptor.getValue().get(0);

        assertThat(response.getMemoryId()).isEqualTo(101L);
        assertThat(response.getDocumentId()).isEqualTo("101");
        assertThat(response.getStatus()).isEqualTo("UPSERTED");
        assertThat(document.getId()).isEqualTo("important decision\nuse cards layout\ndetail");
        assertThat(document.getText()).isEqualTo("101");
        assertThat(document.getMetadata()).containsEntry("userId", 7L)
                .containsEntry("projectId", 70L)
                .containsEntry("chatId", 700L)
                .containsEntry("scopeType", "project")
                .containsEntry("memoryType", "decision");
    }

    @Test
    void upsertMemoryShouldThrowWhenMemoryRecordDoesNotExist() {
        given(memoryRecordMapper.selectById(999L)).willReturn(null);

        assertThatThrownBy(() -> vectorMemoryService.upsertMemory(999L))
                .hasMessage("memory record not found");
    }

    @Test
    void searchMemoriesShouldMergeChatProjectAndGlobalHitsWithIsolationFilters() {
        given(memoryVectorStore.similaritySearch(any(SearchRequest.class)))
                .willReturn(List.of(document("chat memory", 201L, 7L, 70L, 700L, "chat", "fact", 0.7D)))
                .willReturn(List.of(
                        document("project memory", 202L, 7L, 70L, 0L, "project", "requirement", 0.9D),
                        document("chat memory duplicate", 201L, 7L, 70L, 700L, "chat", "fact", 0.7D)))
                .willReturn(List.of(document("global memory", 203L, 7L, 99L, 0L, "global", "preference", 0.8D)));

        VectorMemorySearchResponseVO response = vectorMemoryService.searchMemories(
                7L,
                70L,
                700L,
                List.of("CHAT", "PROJECT", "GLOBAL"),
                "query",
                3);

        verify(memoryVectorStore, org.mockito.Mockito.times(3)).similaritySearch(searchRequestCaptor.capture());
        List<SearchRequest> requests = searchRequestCaptor.getAllValues();
        PrintFilterExpressionConverter converter = new PrintFilterExpressionConverter();

        assertThat(response.getTotalHits()).isEqualTo(3);
        assertThat(response.getProjectHits()).isEqualTo(2);
        assertThat(response.getSupplementalHits()).isEqualTo(1);
        assertThat(response.getHits()).extracting(hit -> hit.getMemoryId())
                .containsExactly(201L, 202L, 203L);

        String chatFilter = converter.convertExpression(requests.get(0).getFilterExpression());
        String projectFilter = converter.convertExpression(requests.get(1).getFilterExpression());
        String supplementalFilter = converter.convertExpression(requests.get(2).getFilterExpression());

        assertThat(chatFilter).contains("userId").contains("7").contains("projectId").contains("70").contains("chatId").contains("700");
        assertThat(projectFilter).contains("userId").contains("7").contains("projectId").contains("70").doesNotContain("chatId");
        assertThat(supplementalFilter).contains("userId").contains("7").contains("projectId").contains("70").contains("global");
    }

    @Test
    void searchForRagShouldUseSeparatedProjectChatAndGlobalFiltersUnderMockVectorStore() {
        given(memoryVectorStore.similaritySearch(any(SearchRequest.class)))
                .willReturn(List.of(document("current project rule", 301L, 8L, 80L, 0L, "project", "requirement", 0.95D)))
                .willReturn(List.of(document("current chat fact", 302L, 8L, 80L, 800L, "chat", "fact", 0.6D)))
                .willReturn(List.of(document("global user preference", 303L, 8L, 0L, 0L, "global", "preference", 0.8D)));

        RagMemorySearchResult result = vectorMemoryService.searchForRag(8L, 80L, 800L, "home page copy", 2);

        verify(memoryVectorStore, org.mockito.Mockito.times(3)).similaritySearch(searchRequestCaptor.capture());
        List<SearchRequest> requests = searchRequestCaptor.getAllValues();
        PrintFilterExpressionConverter converter = new PrintFilterExpressionConverter();

        assertThat(result.getProjectMemories()).extracting(hit -> hit.getMemoryId()).containsExactly(301L);
        assertThat(result.getChatMemories()).extracting(hit -> hit.getMemoryId()).containsExactly(302L);
        assertThat(result.getGlobalMemories()).extracting(hit -> hit.getMemoryId()).containsExactly(303L);

        String projectFilter = converter.convertExpression(requests.get(0).getFilterExpression());
        String chatFilter = converter.convertExpression(requests.get(1).getFilterExpression());
        String globalFilter = converter.convertExpression(requests.get(2).getFilterExpression());

        assertThat(projectFilter).contains("projectId").contains("80").contains("scopeType").contains("project");
        assertThat(chatFilter).contains("projectId").contains("80").contains("chatId").contains("800").contains("scopeType").contains("chat");
        assertThat(globalFilter).contains("scopeType").contains("global").contains("preference").contains("profile").contains("habit");
        assertThat(globalFilter).doesNotContain("projectId");
    }

    @Test
    void searchMemoriesShouldReturnEmptyWhenMockVectorStoreHasNoResults() {
        given(memoryVectorStore.similaritySearch(any(SearchRequest.class))).willReturn(List.of());

        VectorMemorySearchResponseVO response = vectorMemoryService.searchMemories(
                9L,
                90L,
                900L,
                List.of("PROJECT", "GLOBAL"),
                "nothing",
                null);

        assertThat(response.getTopK()).isEqualTo(3);
        assertThat(response.getTotalHits()).isZero();
        assertThat(response.getProjectHits()).isZero();
        assertThat(response.getSupplementalHits()).isZero();
        assertThat(response.getHits()).isEmpty();
    }

    private MemoryRecord buildMemoryRecord(
            Long id,
            Long userId,
            Long projectId,
            Long chatId,
            String scope,
            String memoryType,
            String title,
            String summary,
            String detail) {
        MemoryRecord record = new MemoryRecord();
        record.setId(id);
        record.setUserAccountId(userId);
        record.setProjectSpaceId(projectId);
        record.setSessionId(chatId);
        record.setMemoryScope(scope);
        record.setMemoryType(memoryType);
        record.setTitle(title);
        record.setSummary(summary);
        record.setDetailText(detail);
        record.setImportanceScore(0.95D);
        record.setCreatedAt(1_700_002_000_000L);
        return record;
    }

    private Document document(
            String text,
            Long memoryId,
            Long userId,
            Long projectId,
            Long chatId,
            String scopeType,
            String memoryType,
            Double importance) {
        return new Document(
                text,
                memoryId.toString(),
                Map.of(
                        "memoryId", memoryId,
                        "userId", userId,
                        "projectId", projectId,
                        "chatId", chatId,
                        "scopeType", scopeType,
                        "memoryType", memoryType,
                        "importance", importance,
                        "createdAt", 1_700_002_100_000L));
    }
}
