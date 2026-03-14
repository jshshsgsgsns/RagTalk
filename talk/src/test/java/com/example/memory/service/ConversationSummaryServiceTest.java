package com.example.memory.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.memory.entity.ConversationSummary;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:target/conversation-summary-service-test.db",
        "app.vector.qdrant.enabled=false"
})
class ConversationSummaryServiceTest {

    @Autowired
    private ConversationSummaryService conversationSummaryService;

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
    void generateSummaryShouldPersistSummaryForChatAndRange() {
        long baseTime = 1_700_005_000_000L;
        seedUser(1L, baseTime);
        seedProject(10L, 1L, baseTime);
        seedChat(100L, 1L, 10L, baseTime);
        seedMessage(1001L, 100L, 10L, 1L, 1, "USER", "先讨论登录流程", baseTime + 1);
        seedMessage(1002L, 100L, 10L, 1L, 2, "ASSISTANT", "建议补充验证码", baseTime + 2);

        ConversationSummary summary = conversationSummaryService.generateSummary(100L, 1, 2);

        assertThat(summary.getId()).isNotNull();
        assertThat(summary.getSummaryVersion()).isEqualTo(1);
        assertThat(summary.getProjectSpaceId()).isEqualTo(10L);
        assertThat(summary.getSummaryText()).contains("Summary(1-2)");
        assertThat(summary.getSummaryText()).contains("[1] USER: 先讨论登录流程");
        assertThat(summary.getSummaryText()).contains("[2] ASSISTANT: 建议补充验证码");

        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT session_id, project_space_id, summary_version, source_start_sequence, source_end_sequence, provider, model_name
                FROM conversation_summary
                WHERE id = ?
                """, summary.getId());
        assertThat(((Number) row.get("session_id")).longValue()).isEqualTo(100L);
        assertThat(((Number) row.get("project_space_id")).longValue()).isEqualTo(10L);
        assertThat(((Number) row.get("summary_version")).intValue()).isEqualTo(1);
        assertThat(((Number) row.get("source_start_sequence")).intValue()).isEqualTo(1);
        assertThat(((Number) row.get("source_end_sequence")).intValue()).isEqualTo(2);
        assertThat(row.get("provider")).isEqualTo("RULE");
        assertThat(row.get("model_name")).isEqualTo("rule-based-summary");
    }

    @Test
    void generateSummaryShouldUpdateExistingSummaryWhenRangeMatches() {
        long baseTime = 1_700_005_100_000L;
        seedUser(2L, baseTime);
        seedProject(20L, 2L, baseTime);
        seedChat(200L, 2L, 20L, baseTime);
        seedMessage(2001L, 200L, 20L, 2L, 1, "USER", "第一轮需求确认", baseTime + 1);
        seedMessage(2002L, 200L, 20L, 2L, 2, "ASSISTANT", "先记录关键约束", baseTime + 2);

        ConversationSummary first = conversationSummaryService.generateSummary(200L, 1, 2);
        jdbcTemplate.update("UPDATE message_event SET content_text = ? WHERE id = ?", "先记录关键约束和范围", 2002L);

        ConversationSummary updated = conversationSummaryService.generateSummary(200L, 1, 2);

        assertThat(updated.getId()).isEqualTo(first.getId());
        assertThat(updated.getSummaryVersion()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM conversation_summary", Integer.class)).isEqualTo(1);

        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT summary_text, updated_at
                FROM conversation_summary
                WHERE id = ?
                """, first.getId());
        assertThat((String) row.get("summary_text")).contains("先记录关键约束和范围");
        assertThat(((Number) row.get("updated_at")).longValue()).isGreaterThanOrEqualTo(first.getUpdatedAt());
    }

    @Test
    void generateSummaryShouldAppendNewVersionForDifferentRange() {
        long baseTime = 1_700_005_200_000L;
        seedUser(3L, baseTime);
        seedProject(30L, 3L, baseTime);
        seedChat(300L, 3L, 30L, baseTime);
        seedMessage(3001L, 300L, 30L, 3L, 1, "USER", "确定项目目标", baseTime + 1);
        seedMessage(3002L, 300L, 30L, 3L, 2, "ASSISTANT", "记录目标与里程碑", baseTime + 2);
        seedMessage(3003L, 300L, 30L, 3L, 3, "USER", "补充验收范围", baseTime + 3);

        ConversationSummary first = conversationSummaryService.generateSummary(300L, 1, 2);
        ConversationSummary second = conversationSummaryService.generateSummary(300L, 2, 3);

        assertThat(first.getSummaryVersion()).isEqualTo(1);
        assertThat(second.getSummaryVersion()).isEqualTo(2);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT summary_version, source_start_sequence, source_end_sequence
                FROM conversation_summary
                WHERE session_id = ?
                ORDER BY summary_version
                """, 300L);
        assertThat(rows).hasSize(2);
        assertThat(((Number) rows.get(0).get("summary_version")).intValue()).isEqualTo(1);
        assertThat(((Number) rows.get(1).get("summary_version")).intValue()).isEqualTo(2);
        assertThat(((Number) rows.get(0).get("source_start_sequence")).intValue()).isEqualTo(1);
        assertThat(((Number) rows.get(1).get("source_start_sequence")).intValue()).isEqualTo(2);
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
}
