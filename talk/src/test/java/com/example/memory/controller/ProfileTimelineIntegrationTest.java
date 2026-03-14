package com.example.memory.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        "spring.datasource.url=jdbc:sqlite:target/profile-timeline-integration-test.db",
        "app.vector.qdrant.enabled=false"
})
@AutoConfigureMockMvc
class ProfileTimelineIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    void profileShouldAggregateGlobalMemoriesIntoStructuredBuckets() throws Exception {
        long baseTime = 1_700_004_000_000L;
        seedUser(1L, baseTime);
        seedUser(2L, baseTime);
        seedProject(10L, 1L, "alpha", "Alpha Project", "alpha desc", baseTime);

        seedMemoryRecord(1001L, 1L, 10L, null, "global", "preference",
                "pref", "short answers", "short answers", baseTime + 10, baseTime + 10, 0.95D, 0.8D);
        seedMemoryRecord(1002L, 1L, 10L, null, "global", "habit",
                "habit", "uses spring boot", "uses Spring Boot with Java", baseTime + 11, baseTime + 11, 0.90D, 0.8D);
        seedMemoryRecord(1003L, 1L, 10L, null, "global", "profile",
                "profile", "builder", "content platform owner", baseTime + 12, baseTime + 12, 0.85D, 0.8D);
        seedMemoryRecord(1004L, 1L, 10L, null, "project", "requirement",
                "project-only", "current project only", "must stay in project", baseTime + 13, baseTime + 13, 0.99D, 0.8D);
        seedMemoryRecord(2001L, 2L, 10L, null, "global", "preference",
                "other-user", "other user memory", "other user memory", baseTime + 14, baseTime + 14, 0.99D, 0.8D);

        mockMvc.perform(get("/api/profile/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.preferences[0]").value("short answers"))
                .andExpect(jsonPath("$.data.longTermHabits[0]").value("uses Spring Boot with Java"))
                .andExpect(jsonPath("$.data.commonProjectDirections[0]").value("content platform owner"))
                .andExpect(jsonPath("$.data.commonTechPreferences[0]").value("uses Spring Boot with Java"));

        Map<String, Object> responseRow = jdbcTemplate.queryForMap("""
                SELECT COUNT(1) AS total_global_memories
                FROM memory_record
                WHERE user_account_id = ? AND memory_scope = 'global'
                """, 1L);
        assertThat(((Number) responseRow.get("total_global_memories")).intValue()).isEqualTo(3);
    }

    @Test
    void timelineShouldReturnUnifiedSortedEventsAcrossProjectsForSameUser() throws Exception {
        long baseTime = 1_700_004_100_000L;
        seedUser(11L, baseTime);
        seedUser(12L, baseTime);

        seedProject(110L, 11L, "p-a", "Project A", "desc A", baseTime + 10);
        seedProject(111L, 11L, "p-b", "Project B", "desc B", baseTime + 20);
        seedProject(999L, 12L, "p-x", "Other Project", "other desc", baseTime + 30);

        seedChat(210L, 11L, 110L, "chat-a", "Chat A", baseTime + 40);
        seedChat(211L, 11L, 111L, "chat-b", "Chat B", baseTime + 50);
        seedChat(998L, 12L, 999L, "chat-x", "Chat X", baseTime + 60);

        seedMessageEvent(310L, 210L, 110L, 11L, 1, "USER", "first message", baseTime + 70);
        seedMessageEvent(311L, 211L, 111L, 11L, 1, "ASSISTANT", "second message", baseTime + 80);
        seedMessageEvent(997L, 998L, 999L, 12L, 1, "USER", "other user message", baseTime + 90);

        seedMemoryRecord(410L, 11L, 110L, 210L, "project", "decision",
                "decision", "project a decision", "project a decision", baseTime + 100, baseTime + 100, 0.95D, 0.8D);
        seedMemoryRecord(411L, 11L, 111L, 211L, "global", "habit",
                "habit", "user habit", "writes notes daily", baseTime + 110, baseTime + 110, 0.90D, 0.8D);
        seedMemoryRecord(996L, 12L, 999L, 998L, "project", "decision",
                "other", "other memory", "other memory", baseTime + 120, baseTime + 120, 0.90D, 0.8D);

        mockMvc.perform(get("/api/timeline/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(11))
                .andExpect(jsonPath("$.data.totalEvents").value(8))
                .andExpect(jsonPath("$.data.events[0].eventType").value("MEMORY_RECORD"))
                .andExpect(jsonPath("$.data.events[0].sourceId").value(411))
                .andExpect(jsonPath("$.data.events[1].eventType").value("MEMORY_RECORD"))
                .andExpect(jsonPath("$.data.events[1].sourceId").value(410))
                .andExpect(jsonPath("$.data.events[2].eventType").value("MESSAGE_EVENT"))
                .andExpect(jsonPath("$.data.events[2].sourceId").value(311))
                .andExpect(jsonPath("$.data.events[3].eventType").value("MESSAGE_EVENT"))
                .andExpect(jsonPath("$.data.events[3].sourceId").value(310))
                .andExpect(jsonPath("$.data.events[4].eventType").value("CHAT_SESSION"))
                .andExpect(jsonPath("$.data.events[4].sourceId").value(211))
                .andExpect(jsonPath("$.data.events[5].eventType").value("CHAT_SESSION"))
                .andExpect(jsonPath("$.data.events[5].sourceId").value(210))
                .andExpect(jsonPath("$.data.events[6].eventType").value("PROJECT_SPACE"))
                .andExpect(jsonPath("$.data.events[6].sourceId").value(111))
                .andExpect(jsonPath("$.data.events[7].eventType").value("PROJECT_SPACE"))
                .andExpect(jsonPath("$.data.events[7].sourceId").value(110))
                .andExpect(jsonPath("$.data.events[0].projectId").value(111))
                .andExpect(jsonPath("$.data.events[1].projectId").value(110))
                .andExpect(jsonPath("$.data.events[2].chatId").value(211))
                .andExpect(jsonPath("$.data.events[3].chatId").value(210));

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT event_type, event_time, project_id, chat_id, source_id
                FROM (
                    SELECT 'PROJECT_SPACE' AS event_type, created_at AS event_time, id AS project_id, NULL AS chat_id, id AS source_id
                    FROM project_space WHERE user_account_id = 11
                    UNION ALL
                    SELECT 'CHAT_SESSION' AS event_type, started_at AS event_time, project_space_id AS project_id, id AS chat_id, id AS source_id
                    FROM chat_session WHERE user_account_id = 11
                    UNION ALL
                    SELECT 'MESSAGE_EVENT' AS event_type, event_time, project_space_id AS project_id, session_id AS chat_id, id AS source_id
                    FROM message_event WHERE user_account_id = 11
                    UNION ALL
                    SELECT 'MEMORY_RECORD' AS event_type, created_at AS event_time, project_space_id AS project_id, session_id AS chat_id, id AS source_id
                    FROM memory_record WHERE user_account_id = 11
                )
                ORDER BY event_time DESC
                """);

        assertThat(rows).hasSize(8);
        assertThat(rows).extracting(row -> row.get("event_type"))
                .containsExactly("MEMORY_RECORD", "MEMORY_RECORD", "MESSAGE_EVENT", "MESSAGE_EVENT", "CHAT_SESSION", "CHAT_SESSION", "PROJECT_SPACE", "PROJECT_SPACE");
        assertThat(rows).extracting(row -> ((Number) row.get("project_id")).longValue())
                .contains(110L, 111L);
        assertThat(rows).extracting(row -> row.get("source_id"))
                .doesNotContain(999L, 998L, 997L, 996L);
    }

    private void seedUser(Long userId, long now) {
        jdbcTemplate.update("""
                INSERT INTO user_account (id, user_code, display_name, account_status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, userId, "user-" + userId, "User " + userId, "ACTIVE", now, now);
    }

    private void seedProject(Long projectId, Long userId, String code, String name, String description, long now) {
        jdbcTemplate.update("""
                INSERT INTO project_space (id, user_account_id, space_code, space_name, memory_scope, description, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, projectId, userId, code, name, "project", description, now, now);
    }

    private void seedChat(Long chatId, Long userId, Long projectId, String code, String title, long now) {
        jdbcTemplate.update("""
                INSERT INTO chat_session (
                    id, user_account_id, project_space_id, session_code, session_title, session_status,
                    started_at, last_message_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, chatId, userId, projectId, code, title, "ACTIVE", now, now, now, now);
    }

    private void seedMessageEvent(
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

    private void seedMemoryRecord(
            Long memoryId,
            Long userId,
            Long projectId,
            Long chatId,
            String scope,
            String memoryType,
            String title,
            String summary,
            String detail,
            long createdAt,
            long updatedAt,
            double importance,
            double confidence) {
        jdbcTemplate.update("""
                INSERT INTO memory_record (
                    id, user_account_id, project_space_id, session_id, memory_scope, memory_type,
                    title, summary, detail_text, importance_score, confidence_score, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                memoryId,
                userId,
                projectId,
                chatId,
                scope,
                memoryType,
                title,
                summary,
                detail,
                importance,
                confidence,
                createdAt,
                updatedAt);
    }
}
