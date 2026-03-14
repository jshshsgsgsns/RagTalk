package com.example.memory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.memory.entity.MemoryRecord;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:target/memory-record-service-dedup-test.db",
        "app.vector.qdrant.enabled=false"
})
class MemoryRecordServiceDedupTest {

    @Autowired
    private MemoryRecordService memoryRecordService;

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
    void saveOrUpdateExtractedMemoryShouldInsertOnFirstWrite() {
        long baseTime = 1_700_002_000_000L;
        seedUser(1L, baseTime);
        seedProject(10L, 1L, baseTime);
        seedChat(100L, 1L, 10L, baseTime);
        seedUserMessage(1001L, 100L, 10L, 1L, 1, "first", baseTime + 1);

        MemoryRecord record = buildRecord(
                1L,
                10L,
                100L,
                1001L,
                MemoryRecordService.SCOPE_PROJECT,
                MemoryRecordService.TYPE_REQUIREMENT,
                "project:req",
                "这个项目需要保留日志",
                "这个项目需要保留完整日志",
                0.8D,
                0.7D,
                "{\"memoryKey\":\"project-logging\"}");

        MemoryRecord saved = memoryRecordService.saveOrUpdateExtractedMemory(record);

        assertThat(saved.getId()).isNotNull();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM memory_record", Integer.class)).isEqualTo(1);
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT summary, detail_text, memory_scope, memory_type, source_message_event_id
                FROM memory_record
                WHERE id = ?
                """, saved.getId());
        assertThat(row.get("summary")).isEqualTo("这个项目需要保留日志");
        assertThat(row.get("detail_text")).isEqualTo("这个项目需要保留完整日志");
        assertThat(row.get("memory_scope")).isEqualTo("project");
        assertThat(row.get("memory_type")).isEqualTo("requirement");
        assertThat(((Number) row.get("source_message_event_id")).longValue()).isEqualTo(1001L);

        verify(vectorMemoryService, times(1)).upsertMemory(any(MemoryRecord.class));
    }

    @Test
    void saveOrUpdateExtractedMemoryShouldUpdateInsteadOfInsertWhenMemoryKeyMatches() {
        long baseTime = 1_700_002_100_000L;
        seedUser(2L, baseTime);
        seedProject(20L, 2L, baseTime);
        seedChat(200L, 2L, 20L, baseTime);
        seedUserMessage(2001L, 200L, 20L, 2L, 1, "first", baseTime + 1);
        seedUserMessage(2002L, 200L, 20L, 2L, 2, "second", baseTime + 2);

        MemoryRecord first = buildRecord(
                2L,
                20L,
                200L,
                2001L,
                MemoryRecordService.SCOPE_GLOBAL,
                MemoryRecordService.TYPE_PREFERENCE,
                "global:preference",
                "用户喜欢简洁界面",
                "用户长期喜欢简洁界面",
                0.75D,
                0.82D,
                "{\"memoryKey\":\"ui-style\"}");
        MemoryRecord inserted = memoryRecordService.saveOrUpdateExtractedMemory(first);

        MemoryRecord second = buildRecord(
                2L,
                20L,
                200L,
                2002L,
                MemoryRecordService.SCOPE_GLOBAL,
                MemoryRecordService.TYPE_PREFERENCE,
                "global:preference",
                "用户偏好极简界面",
                "用户长期偏好极简界面，减少装饰元素",
                0.91D,
                0.78D,
                "{\"memoryKey\":\"ui-style\"}");
        MemoryRecord updated = memoryRecordService.saveOrUpdateExtractedMemory(second);

        assertThat(updated.getId()).isEqualTo(inserted.getId());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM memory_record", Integer.class)).isEqualTo(1);

        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT source_message_event_id, summary, detail_text, importance_score, confidence_score, created_at, updated_at
                FROM memory_record
                WHERE id = ?
                """, inserted.getId());
        assertThat(((Number) row.get("source_message_event_id")).longValue()).isEqualTo(2002L);
        assertThat(row.get("summary")).isEqualTo("用户偏好极简界面");
        assertThat(row.get("detail_text")).isEqualTo("用户长期偏好极简界面，减少装饰元素");
        assertThat(((Number) row.get("importance_score")).doubleValue()).isEqualTo(0.91D);
        assertThat(((Number) row.get("confidence_score")).doubleValue()).isEqualTo(0.82D);
        assertThat(((Number) row.get("updated_at")).longValue()).isGreaterThanOrEqualTo(((Number) row.get("created_at")).longValue());

        verify(vectorMemoryService, times(2)).upsertMemory(any(MemoryRecord.class));
    }

    @Test
    void saveOrUpdateExtractedMemoryShouldNotMergeAcrossProjectsForProjectScope() {
        long baseTime = 1_700_002_200_000L;
        seedUser(3L, baseTime);
        seedProject(30L, 3L, baseTime);
        seedProject(31L, 3L, baseTime + 1);
        seedChat(300L, 3L, 30L, baseTime);
        seedChat(301L, 3L, 31L, baseTime + 1);
        seedUserMessage(3001L, 300L, 30L, 3L, 1, "first", baseTime + 2);
        seedUserMessage(3011L, 301L, 31L, 3L, 1, "second", baseTime + 3);

        memoryRecordService.saveOrUpdateExtractedMemory(buildRecord(
                3L,
                30L,
                300L,
                3001L,
                MemoryRecordService.SCOPE_PROJECT,
                MemoryRecordService.TYPE_CONSTRAINT,
                "project:constraint",
                "项目A禁止删除历史消息",
                "项目A禁止删除历史消息",
                0.9D,
                0.85D,
                "{\"memoryKey\":\"history-retention\"}"));
        memoryRecordService.saveOrUpdateExtractedMemory(buildRecord(
                3L,
                31L,
                301L,
                3011L,
                MemoryRecordService.SCOPE_PROJECT,
                MemoryRecordService.TYPE_CONSTRAINT,
                "project:constraint",
                "项目B禁止删除历史消息",
                "项目B禁止删除历史消息",
                0.92D,
                0.86D,
                "{\"memoryKey\":\"history-retention\"}"));

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT project_space_id, summary
                FROM memory_record
                ORDER BY project_space_id
                """);

        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(row -> ((Number) row.get("project_space_id")).longValue())
                .containsExactly(30L, 31L);
    }

    @Test
    void saveOrUpdateExtractedMemoryShouldNotMergeAcrossScopes() {
        long baseTime = 1_700_002_300_000L;
        seedUser(4L, baseTime);
        seedProject(40L, 4L, baseTime);
        seedChat(400L, 4L, 40L, baseTime);
        seedUserMessage(4001L, 400L, 40L, 4L, 1, "first", baseTime + 1);
        seedUserMessage(4002L, 400L, 40L, 4L, 2, "second", baseTime + 2);

        memoryRecordService.saveOrUpdateExtractedMemory(buildRecord(
                4L,
                40L,
                400L,
                4001L,
                MemoryRecordService.SCOPE_GLOBAL,
                MemoryRecordService.TYPE_HABIT,
                "global:habit",
                "用户通常早上整理需求",
                "用户通常早上整理需求",
                0.85D,
                0.8D,
                "{\"memoryKey\":\"morning-routine\"}"));
        memoryRecordService.saveOrUpdateExtractedMemory(buildRecord(
                4L,
                40L,
                400L,
                4002L,
                MemoryRecordService.SCOPE_CHAT,
                MemoryRecordService.TYPE_HABIT,
                "chat:habit",
                "本次对话提到早上整理需求",
                "本次对话提到早上整理需求",
                0.7D,
                0.75D,
                "{\"memoryKey\":\"morning-routine\"}"));

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT memory_scope, summary
                FROM memory_record
                ORDER BY memory_scope
                """);

        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(row -> row.get("memory_scope"))
                .containsExactly("chat", "global");
    }

    private MemoryRecord buildRecord(
            Long userId,
            Long projectId,
            Long chatId,
            Long sourceMessageEventId,
            String scope,
            String memoryType,
            String title,
            String summary,
            String detailText,
            Double importance,
            Double confidence,
            String metadataJson) {
        MemoryRecord record = new MemoryRecord();
        record.setUserAccountId(userId);
        record.setProjectSpaceId(projectId);
        record.setSessionId(chatId);
        record.setSourceMessageEventId(sourceMessageEventId);
        record.setMemoryScope(scope);
        record.setMemoryType(memoryType);
        record.setTitle(title);
        record.setSummary(summary);
        record.setDetailText(detailText);
        record.setImportanceScore(importance);
        record.setConfidenceScore(confidence);
        record.setMetadataJson(metadataJson);
        return record;
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
