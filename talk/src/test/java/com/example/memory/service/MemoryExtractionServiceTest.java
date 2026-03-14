package com.example.memory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.memory.vo.memory.ExtractMemoryResponseVO;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:target/memory-extraction-service-test.db",
        "app.vector.qdrant.enabled=false"
})
class MemoryExtractionServiceTest {

    private static final String PROFILE_TEXT = "\u6211\u662f\u4ea7\u54c1\u7ecf\u7406";
    private static final String PREFERENCE_TEXT = "\u6211\u559c\u6b22\u7b80\u6d01\u7684\u754c\u9762";
    private static final String HABIT_TEXT = "\u6211\u4ee5\u540e\u901a\u5e38\u5728\u65e9\u4e0a\u6574\u7406\u9700\u6c42";
    private static final String REQUIREMENT_TEXT = "\u8fd9\u4e2a\u9879\u76ee\u9700\u8981\u4fdd\u7559\u5b8c\u6574\u5ba1\u8ba1\u65e5\u5fd7";
    private static final String CONSTRAINT_TEXT = "\u8fd9\u4e2a\u9879\u76ee\u4e0d\u5141\u8bb8\u5220\u9664\u5386\u53f2\u6d88\u606f";
    private static final String DECISION_TEXT = "\u8fd9\u4e2a\u9879\u76ee\u6700\u7ec8\u786e\u5b9a\u4f7f\u7528 SQLite";

    @Autowired
    private MemoryExtractionService memoryExtractionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private VectorMemoryService vectorMemoryService;

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
    void extractFromChatShouldRecognizePreferenceProfileAndHabitAndPersistRecords() {
        long baseTime = 1_700_001_000_000L;
        seedUser(1L, baseTime);
        seedProject(10L, 1L, baseTime);
        seedChat(100L, 1L, 10L, baseTime);
        seedUserMessage(1001L, 100L, 10L, 1L, 1, PROFILE_TEXT, baseTime + 1);
        seedUserMessage(1002L, 100L, 10L, 1L, 2, PREFERENCE_TEXT, baseTime + 2);
        seedUserMessage(1003L, 100L, 10L, 1L, 3, HABIT_TEXT, baseTime + 3);

        ExtractMemoryResponseVO response = memoryExtractionService.extractFromChat(100L, 10L);

        assertThat(response.getScannedMessageCount()).isEqualTo(3);
        assertThat(response.getExtractedCount()).isEqualTo(3);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT source_message_event_id, memory_type, memory_scope, summary
                FROM memory_record
                ORDER BY source_message_event_id
                """);

        assertThat(rows).hasSize(3);
        assertThat(rows).extracting(row -> row.get("memory_type"))
                .containsExactly("profile", "preference", "habit");
        assertThat(rows).extracting(row -> row.get("memory_scope"))
                .containsExactly("chat", "chat", "global");
        assertThat(rows).extracting(row -> row.get("summary"))
                .containsExactly(PROFILE_TEXT, PREFERENCE_TEXT, HABIT_TEXT);

        verify(vectorMemoryService, times(3)).upsertMemory(any(com.example.memory.entity.MemoryRecord.class));
    }

    @Test
    void extractFromChatShouldRecognizeRequirementAndConstraintAndPersistRecords() {
        long baseTime = 1_700_001_100_000L;
        seedUser(2L, baseTime);
        seedProject(20L, 2L, baseTime);
        seedChat(200L, 2L, 20L, baseTime);
        seedUserMessage(2001L, 200L, 20L, 2L, 1, REQUIREMENT_TEXT, baseTime + 1);
        seedUserMessage(2002L, 200L, 20L, 2L, 2, CONSTRAINT_TEXT, baseTime + 2);

        ExtractMemoryResponseVO response = memoryExtractionService.extractFromChat(200L, 20L);

        assertThat(response.getExtractedCount()).isEqualTo(2);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT source_message_event_id, memory_type, memory_scope, importance_score
                FROM memory_record
                ORDER BY source_message_event_id
                """);

        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(row -> row.get("memory_type"))
                .containsExactly("requirement", "constraint");
        assertThat(rows).extracting(row -> row.get("memory_scope"))
                .containsExactly("project", "project");
        assertThat(((Number) rows.get(0).get("importance_score")).doubleValue()).isEqualTo(0.9D);
        assertThat(((Number) rows.get(1).get("importance_score")).doubleValue()).isEqualTo(0.95D);
    }

    @Test
    void extractFromChatShouldRecognizeDecisionAndPersistRecord() {
        long baseTime = 1_700_001_200_000L;
        seedUser(3L, baseTime);
        seedProject(30L, 3L, baseTime);
        seedChat(300L, 3L, 30L, baseTime);
        seedUserMessage(3001L, 300L, 30L, 3L, 1, DECISION_TEXT, baseTime + 1);

        ExtractMemoryResponseVO response = memoryExtractionService.extractFromChat(300L, 30L);

        assertThat(response.getExtractedCount()).isEqualTo(1);

        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT memory_type, memory_scope, title, importance_score
                FROM memory_record
                WHERE source_message_event_id = ?
                """, 3001L);

        assertThat(row.get("memory_type")).isEqualTo("decision");
        assertThat(row.get("memory_scope")).isEqualTo("project");
        assertThat(row.get("title")).isEqualTo("project:decision");
        assertThat(((Number) row.get("importance_score")).doubleValue()).isEqualTo(0.95D);
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

    private void seedUserMessage(
            Long messageId,
            Long chatId,
            Long projectId,
            Long userId,
            int sequenceNo,
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
                "USER",
                "MESSAGE",
                content,
                "{}",
                "OLLAMA",
                "qwen2.5:7b",
                eventTime,
                eventTime);
    }
}
