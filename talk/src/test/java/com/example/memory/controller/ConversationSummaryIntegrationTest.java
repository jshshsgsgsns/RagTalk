package com.example.memory.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.memory.service.ConversationSummaryService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:target/conversation-summary-integration-test.db",
        "app.vector.qdrant.enabled=false"
})
@AutoConfigureMockMvc
class ConversationSummaryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ConversationSummaryService conversationSummaryService;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM conversation_summary");
        jdbcTemplate.update("DELETE FROM memory_record");
        jdbcTemplate.update("DELETE FROM message_event");
        jdbcTemplate.update("DELETE FROM chat_session");
        jdbcTemplate.update("DELETE FROM project_space");
        jdbcTemplate.update("DELETE FROM user_account");
    }

    @Test
    void timelineShouldIncludeConversationSummaryEventsAfterSummaryGeneration() throws Exception {
        long baseTime = 1_700_005_300_000L;
        seedUser(11L, baseTime);
        seedProject(110L, 11L, baseTime);
        seedChat(210L, 11L, 110L, "Chat A", baseTime);
        seedMessage(310L, 210L, 110L, 11L, 1, "USER", "先确认登录流程", baseTime + 10);
        seedMessage(311L, 210L, 110L, 11L, 2, "ASSISTANT", "补充验证码和风控", baseTime + 20);

        conversationSummaryService.generateSummary(210L, 1, 2);

        mockMvc.perform(get("/api/timeline/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalEvents").value(5))
                .andExpect(jsonPath("$.data.events[0].eventType").value("CONVERSATION_SUMMARY"))
                .andExpect(jsonPath("$.data.events[0].projectId").value(110))
                .andExpect(jsonPath("$.data.events[0].chatId").value(210))
                .andExpect(jsonPath("$.data.events[0].title").value("summary-v1"))
                .andExpect(jsonPath("$.data.events[0].summary").value(org.hamcrest.Matchers.containsString("Summary(1-2)")))
                .andExpect(jsonPath("$.data.events[0].detail").value("rule-based-summary"));

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT event_type, source_id
                FROM (
                    SELECT 'PROJECT_SPACE' AS event_type, id AS source_id, created_at AS event_time
                    FROM project_space WHERE user_account_id = 11
                    UNION ALL
                    SELECT 'CHAT_SESSION' AS event_type, id AS source_id, started_at AS event_time
                    FROM chat_session WHERE user_account_id = 11
                    UNION ALL
                    SELECT 'MESSAGE_EVENT' AS event_type, id AS source_id, event_time
                    FROM message_event WHERE user_account_id = 11
                    UNION ALL
                    SELECT 'CONVERSATION_SUMMARY' AS event_type, id AS source_id, created_at AS event_time
                    FROM conversation_summary WHERE project_space_id = 110
                )
                ORDER BY event_time DESC
                """);
        assertThat(rows).hasSize(5);
        assertThat(rows.get(0).get("event_type")).isEqualTo("CONVERSATION_SUMMARY");
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

    private void seedChat(Long chatId, Long userId, Long projectId, String title, long now) {
        jdbcTemplate.update("""
                INSERT INTO chat_session (
                    id, user_account_id, project_space_id, session_code, session_title, session_status,
                    started_at, last_message_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, chatId, userId, projectId, "chat-" + chatId, title, "ACTIVE", now, now, now, now);
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
}
