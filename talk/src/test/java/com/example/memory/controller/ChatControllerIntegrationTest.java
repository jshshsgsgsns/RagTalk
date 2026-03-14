package com.example.memory.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:target/chat-controller-integration-test.db",
        "app.vector.qdrant.enabled=false"
})
@AutoConfigureMockMvc
class ChatControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private ChatClient chatClient;

    private ChatClientRequestSpec requestSpec;
    private CallResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        requestSpec = mock(ChatClientRequestSpec.class);
        responseSpec = mock(CallResponseSpec.class);

        given(chatClient.prompt()).willReturn(requestSpec);
        given(requestSpec.messages(any(List.class))).willReturn(requestSpec);
        given(requestSpec.call()).willReturn(responseSpec);

        jdbcTemplate.update("DELETE FROM conversation_summary");
        jdbcTemplate.update("DELETE FROM memory_record");
        jdbcTemplate.update("DELETE FROM message_event");
        jdbcTemplate.update("DELETE FROM chat_session");
        jdbcTemplate.update("DELETE FROM project_space");
        jdbcTemplate.update("DELETE FROM user_account");
    }

    @Test
    void sendShouldPersistUserAndAssistantMessagesAdvanceSequenceAndUpdateSessionTimestamp() throws Exception {
        long baseTime = 1_700_000_000_000L;
        seedUser(1L, baseTime);
        seedProject(10L, 1L, "project-a", "Project A", baseTime);
        seedChat(100L, 1L, 10L, "chat-100", "ACTIVE", baseTime, baseTime);
        seedMessageEvent(1000L, 100L, 10L, 1L, 1, "ASSISTANT", "历史回复", baseTime + 1);

        given(responseSpec.content()).willReturn("新的 AI 回复");

        mockMvc.perform(post("/api/chat/send")
                        .contentType("application/json")
                        .content("""
                                {
                                  "projectId": 10,
                                  "chatId": 100,
                                  "message": "请用中文回答"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.projectId").value(10))
                .andExpect(jsonPath("$.data.chatId").value(100))
                .andExpect(jsonPath("$.data.userSequenceNo").value(2))
                .andExpect(jsonPath("$.data.assistantSequenceNo").value(3))
                .andExpect(jsonPath("$.data.assistantReply").value("新的 AI 回复"));

        List<Map<String, Object>> insertedEvents = jdbcTemplate.queryForList("""
                SELECT sequence_no, role, content_text
                FROM message_event
                WHERE session_id = ? AND sequence_no >= 2
                ORDER BY sequence_no
                """, 100L);

        assertThat(insertedEvents).hasSize(2);
        assertThat(((Number) insertedEvents.get(0).get("sequence_no")).intValue()).isEqualTo(2);
        assertThat(insertedEvents.get(0).get("role")).isEqualTo("USER");
        assertThat(insertedEvents.get(0).get("content_text")).isEqualTo("请用中文回答");
        assertThat(((Number) insertedEvents.get(1).get("sequence_no")).intValue()).isEqualTo(3);
        assertThat(insertedEvents.get(1).get("role")).isEqualTo("ASSISTANT");
        assertThat(insertedEvents.get(1).get("content_text")).isEqualTo("新的 AI 回复");

        Long updatedAt = jdbcTemplate.queryForObject(
                "SELECT updated_at FROM chat_session WHERE id = ?",
                Long.class,
                100L);
        Long lastMessageAt = jdbcTemplate.queryForObject(
                "SELECT last_message_at FROM chat_session WHERE id = ?",
                Long.class,
                100L);

        assertThat(updatedAt).isNotNull().isGreaterThan(baseTime);
        assertThat(lastMessageAt).isNotNull().isGreaterThan(baseTime);
    }

    @Test
    void sendShouldRejectWhenChatSessionDoesNotBelongToProjectSpace() throws Exception {
        long baseTime = 1_700_000_100_000L;
        seedUser(2L, baseTime);
        seedProject(20L, 2L, "project-a", "Project A", baseTime);
        seedProject(21L, 2L, "project-b", "Project B", baseTime);
        seedChat(200L, 2L, 21L, "chat-200", "ACTIVE", baseTime, baseTime);

        mockMvc.perform(post("/api/chat/send")
                        .contentType("application/json")
                        .content("""
                                {
                                  "projectId": 20,
                                  "chatId": 200,
                                  "message": "这次写入必须失败"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("chat session does not belong to project space"));

        Integer messageCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM message_event WHERE session_id = ?",
                Integer.class,
                200L);
        Long updatedAt = jdbcTemplate.queryForObject(
                "SELECT updated_at FROM chat_session WHERE id = ?",
                Long.class,
                200L);

        assertThat(messageCount).isZero();
        assertThat(updatedAt).isEqualTo(baseTime);
    }

    @Test
    void sendShouldRejectCrossProjectWriteWithoutPersistingAnyMessage() throws Exception {
        long baseTime = 1_700_000_200_000L;
        seedUser(3L, baseTime);
        seedUser(4L, baseTime);
        seedProject(30L, 3L, "project-user-a", "User A Project", baseTime);
        seedProject(40L, 4L, "project-user-b", "User B Project", baseTime);
        seedChat(400L, 4L, 40L, "chat-400", "ACTIVE", baseTime, baseTime);

        mockMvc.perform(post("/api/chat/send")
                        .contentType("application/json")
                        .content("""
                                {
                                  "projectId": 30,
                                  "chatId": 400,
                                  "message": "跨项目写入"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("chat session does not belong to project space"));

        Integer totalMessageCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM message_event",
                Integer.class);
        Long updatedAt = jdbcTemplate.queryForObject(
                "SELECT updated_at FROM chat_session WHERE id = ?",
                Long.class,
                400L);

        assertThat(totalMessageCount).isZero();
        assertThat(updatedAt).isEqualTo(baseTime);
    }

    private void seedUser(Long userId, long now) {
        jdbcTemplate.update("""
                INSERT INTO user_account (id, user_code, display_name, account_status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, userId, "user-" + userId, "User " + userId, "ACTIVE", now, now);
    }

    private void seedProject(Long projectId, Long userId, String spaceCode, String spaceName, long now) {
        jdbcTemplate.update("""
                INSERT INTO project_space (id, user_account_id, space_code, space_name, memory_scope, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, projectId, userId, spaceCode, spaceName, "project", now, now);
    }

    private void seedChat(
            Long chatId,
            Long userId,
            Long projectId,
            String sessionCode,
            String sessionStatus,
            long startedAt,
            long updatedAt) {
        jdbcTemplate.update("""
                INSERT INTO chat_session (
                    id, user_account_id, project_space_id, session_code, session_status,
                    started_at, last_message_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, chatId, userId, projectId, sessionCode, sessionStatus, startedAt, startedAt, startedAt, updatedAt);
    }

    private void seedMessageEvent(
            Long messageId,
            Long chatId,
            Long projectId,
            Long userId,
            int sequenceNo,
            String role,
            String contentText,
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
                contentText,
                "{}",
                "OLLAMA",
                "qwen2.5:7b",
                eventTime,
                eventTime);
    }
}
