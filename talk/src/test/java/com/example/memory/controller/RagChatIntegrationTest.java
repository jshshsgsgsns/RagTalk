package com.example.memory.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.converter.PrintFilterExpressionConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:target/rag-chat-integration-test.db",
        "app.vector.qdrant.enabled=false"
})
@AutoConfigureMockMvc
class RagChatIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private ChatClient chatClient;

    @MockBean(name = "memoryVectorStore")
    private VectorStore memoryVectorStore;

    private ChatClientRequestSpec requestSpec;
    private CallResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        requestSpec = mock(ChatClientRequestSpec.class);
        responseSpec = mock(CallResponseSpec.class);

        given(chatClient.prompt()).willReturn(requestSpec);
        given(requestSpec.call()).willReturn(responseSpec);

        jdbcTemplate.update("DELETE FROM conversation_summary");
        jdbcTemplate.update("DELETE FROM memory_record");
        jdbcTemplate.update("DELETE FROM message_event");
        jdbcTemplate.update("DELETE FROM chat_session");
        jdbcTemplate.update("DELETE FROM project_space");
        jdbcTemplate.update("DELETE FROM user_account");
    }

    @Test
    void ragSendShouldBuildRetrievedContextPersistMessagesAndReturnRagResponse() throws Exception {
        long baseTime = 1_700_003_000_000L;
        seedUser(21L, baseTime);
        seedProject(210L, 21L, baseTime);
        seedProject(211L, 21L, baseTime);
        seedChat(310L, 21L, 210L, baseTime);
        seedMessage(3101L, 310L, 210L, 21L, 1, "USER", "history question", baseTime + 1);
        seedMessage(3102L, 310L, 210L, 21L, 2, "ASSISTANT", "history answer", baseTime + 2);

        given(memoryVectorStore.similaritySearch(any(SearchRequest.class)))
                .willReturn(List.of(document("current project requires chinese output", 801L, 21L, 210L, 0L, "project", "requirement", 0.95D)))
                .willReturn(List.of(document("current chat background", 802L, 21L, 210L, 310L, "chat", "fact", 0.70D)))
                .willReturn(List.of(document("user prefers concise answers", 803L, 21L, 0L, 0L, "global", "preference", 0.80D)));

        ArgumentCaptor<List<Message>> messagesCaptor = ArgumentCaptor.forClass(List.class);
        given(requestSpec.messages(messagesCaptor.capture())).willReturn(requestSpec);
        given(responseSpec.content()).willReturn("rag reply");

        mockMvc.perform(post("/api/chat/rag-send")
                        .contentType("application/json")
                        .content("""
                                {
                                  "projectId": 210,
                                  "chatId": 310,
                                  "message": "write home page copy"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.projectId").value(210))
                .andExpect(jsonPath("$.data.chatId").value(310))
                .andExpect(jsonPath("$.data.usedMemoryCount").value(3))
                .andExpect(jsonPath("$.data.assistantReply").value("rag reply"))
                .andExpect(jsonPath("$.data.retrievedMemories.projectMemories[0].memoryId").value(801))
                .andExpect(jsonPath("$.data.retrievedMemories.chatMemories[0].memoryId").value(802))
                .andExpect(jsonPath("$.data.retrievedMemories.globalMemories[0].memoryId").value(803));

        List<Message> promptMessages = messagesCaptor.getValue();
        String systemPrompt = ((SystemMessage) promptMessages.get(0)).getText();

        assertThat(systemPrompt).contains("Project memories:")
                .contains("memoryId=801")
                .contains("Chat memories:")
                .contains("memoryId=802")
                .contains("Global memories:")
                .contains("memoryId=803");

        List<Integer> sequenceNos = jdbcTemplate.queryForList("""
                SELECT sequence_no
                FROM message_event
                WHERE session_id = ?
                ORDER BY sequence_no
                """, Integer.class, 310L);
        String assistantReply = jdbcTemplate.queryForObject("""
                SELECT content_text
                FROM message_event
                WHERE session_id = ? AND role = 'ASSISTANT'
                ORDER BY sequence_no DESC
                LIMIT 1
                """, String.class, 310L);

        assertThat(sequenceNos).containsExactly(1, 2, 3, 4);
        assertThat(assistantReply).isEqualTo("rag reply");
    }

    @Test
    void ragSendShouldFallbackToNormalConversationWhenNoMemoryIsRetrieved() throws Exception {
        long baseTime = 1_700_003_100_000L;
        seedUser(22L, baseTime);
        seedProject(220L, 22L, baseTime);
        seedChat(320L, 22L, 220L, baseTime);

        given(memoryVectorStore.similaritySearch(any(SearchRequest.class))).willReturn(List.of());

        ArgumentCaptor<List<Message>> messagesCaptor = ArgumentCaptor.forClass(List.class);
        given(requestSpec.messages(messagesCaptor.capture())).willReturn(requestSpec);
        given(responseSpec.content()).willReturn("fallback reply");

        mockMvc.perform(post("/api/chat/rag-send")
                        .contentType("application/json")
                        .content("""
                                {
                                  "projectId": 220,
                                  "chatId": 320,
                                  "message": "give me a title"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.usedMemoryCount").value(0))
                .andExpect(jsonPath("$.data.retrievedMemories.total").value(0))
                .andExpect(jsonPath("$.data.assistantReply").value("fallback reply"));

        String systemPrompt = ((SystemMessage) messagesCaptor.getValue().get(0)).getText();
        assertThat(systemPrompt).contains("Project memories:\n- none")
                .contains("Chat memories:\n- none")
                .contains("Global memories:\n- none");

        Integer messageCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM message_event WHERE session_id = ?",
                Integer.class,
                320L);
        assertThat(messageCount).isEqualTo(2);
    }

    @Test
    void ragSendShouldKeepOldProjectMemoriesOutOfCurrentProjectConversation() throws Exception {
        long baseTime = 1_700_003_200_000L;
        seedUser(23L, baseTime);
        seedProject(230L, 23L, baseTime);
        seedProject(231L, 23L, baseTime);
        seedChat(330L, 23L, 230L, baseTime);

        given(memoryVectorStore.similaritySearch(any(SearchRequest.class))).willReturn(List.of());

        ArgumentCaptor<SearchRequest> searchCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        given(requestSpec.messages(any(List.class))).willReturn(requestSpec);
        given(responseSpec.content()).willReturn("isolated reply");

        mockMvc.perform(post("/api/chat/rag-send")
                        .contentType("application/json")
                        .content("""
                                {
                                  "projectId": 230,
                                  "chatId": 330,
                                  "message": "continue current project"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.usedMemoryCount").value(0))
                .andExpect(jsonPath("$.data.assistantReply").value("isolated reply"));

        org.mockito.Mockito.verify(memoryVectorStore, org.mockito.Mockito.times(3)).similaritySearch(searchCaptor.capture());
        List<SearchRequest> requests = searchCaptor.getAllValues();
        PrintFilterExpressionConverter converter = new PrintFilterExpressionConverter();

        String projectFilter = converter.convertExpression(requests.get(0).getFilterExpression());
        String chatFilter = converter.convertExpression(requests.get(1).getFilterExpression());
        String globalFilter = converter.convertExpression(requests.get(2).getFilterExpression());

        assertThat(projectFilter).contains("projectId").contains("230").doesNotContain("231");
        assertThat(chatFilter).contains("projectId").contains("230").contains("chatId").contains("330").doesNotContain("231");
        assertThat(globalFilter).contains("scopeType").contains("global").contains("preference").contains("profile").contains("habit");
        assertThat(globalFilter).doesNotContain("projectId");
    }

    private void seedUser(Long userId, long now) {
        jdbcTemplate.update("""
                INSERT INTO user_account (id, user_code, display_name, account_status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, userId, "user-" + userId, "User " + userId, "ACTIVE", now, now);
    }

    private void seedProject(Long projectId, Long userId, long now) {
        jdbcTemplate.update("""
                INSERT INTO project_space (id, user_account_id, space_code, space_name, memory_scope, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, projectId, userId, "project-" + projectId, "Project " + projectId, "project", now, now);
    }

    private void seedChat(Long chatId, Long userId, Long projectId, long now) {
        jdbcTemplate.update("""
                INSERT INTO chat_session (
                    id, user_account_id, project_space_id, session_code, session_status,
                    started_at, last_message_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, chatId, userId, projectId, "chat-" + chatId, "ACTIVE", now, now, now, now);
    }

    private void seedMessage(
            Long messageId,
            Long chatId,
            Long projectId,
            Long userId,
            int sequenceNo,
            String role,
            String content,
            long eventTime) {
        jdbcTemplate.update("""
                INSERT INTO message_event (
                    id, session_id, project_space_id, user_account_id, sequence_no, role,
                    event_type, content_text, content_json, provider, model_name, event_time, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                messageId,
                chatId,
                projectId,
                userId,
                sequenceNo,
                role,
                "MESSAGE",
                content,
                "{}",
                "OLLAMA",
                "qwen2.5:7b",
                eventTime,
                eventTime);
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
                java.util.Map.of(
                        "memoryId", memoryId,
                        "userId", userId,
                        "projectId", projectId,
                        "chatId", chatId,
                        "scopeType", scopeType,
                        "memoryType", memoryType,
                        "importance", importance,
                        "createdAt", 1_700_003_500_000L));
    }
}
