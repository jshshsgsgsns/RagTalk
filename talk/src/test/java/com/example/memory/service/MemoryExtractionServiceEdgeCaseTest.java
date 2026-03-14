package com.example.memory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.memory.exception.BusinessException;
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
        "spring.datasource.url=jdbc:sqlite:target/memory-extraction-service-edge-case-test.db",
        "app.vector.qdrant.enabled=false"
})
class MemoryExtractionServiceEdgeCaseTest {

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
    void extractFromChatShouldThrowWhenChatSessionDoesNotExist() {
        assertThatThrownBy(() -> memoryExtractionService.extractFromChat(999L, 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("chat session not found");

        verify(vectorMemoryService, never()).upsertMemory(any(com.example.memory.entity.MemoryRecord.class));
    }

    @Test
    void extractFromChatShouldThrowWhenChatSessionDoesNotBelongToProjectSpace() {
        long baseTime = 1_700_003_000_000L;
        seedUser(1L, baseTime);
        seedProject(10L, 1L, baseTime);
        seedProject(11L, 1L, baseTime + 1);
        seedChat(100L, 1L, 11L, baseTime + 2);

        assertThatThrownBy(() -> memoryExtractionService.extractFromChat(100L, 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("chat session does not belong to project space");

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM memory_record", Integer.class)).isZero();
        verify(vectorMemoryService, never()).upsertMemory(any(com.example.memory.entity.MemoryRecord.class));
    }

    @Test
    void extractFromChatShouldReturnEmptyResultWhenChatHasNoMessages() {
        long baseTime = 1_700_003_100_000L;
        seedUser(2L, baseTime);
        seedProject(20L, 2L, baseTime);
        seedChat(200L, 2L, 20L, baseTime);

        ExtractMemoryResponseVO response = memoryExtractionService.extractFromChat(200L, 20L);

        assertThat(response.getProjectId()).isEqualTo(20L);
        assertThat(response.getChatId()).isEqualTo(200L);
        assertThat(response.getScannedMessageCount()).isZero();
        assertThat(response.getExtractedCount()).isZero();
        assertThat(response.getMemories()).isEmpty();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM memory_record", Integer.class)).isZero();
        verify(vectorMemoryService, never()).upsertMemory(any(com.example.memory.entity.MemoryRecord.class));
    }

    @Test
    void extractFromChatShouldIgnoreAssistantOnlyMessages() {
        long baseTime = 1_700_003_200_000L;
        seedUser(3L, baseTime);
        seedProject(30L, 3L, baseTime);
        seedChat(300L, 3L, 30L, baseTime);
        seedMessage(3001L, 300L, 30L, 3L, 1, "ASSISTANT", "我是助手建议", baseTime + 1);

        ExtractMemoryResponseVO response = memoryExtractionService.extractFromChat(300L, 30L);

        assertThat(response.getScannedMessageCount()).isZero();
        assertThat(response.getExtractedCount()).isZero();
        assertThat(response.getMemories()).isEmpty();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM memory_record", Integer.class)).isZero();
        verify(vectorMemoryService, never()).upsertMemory(any(com.example.memory.entity.MemoryRecord.class));
    }

    @Test
    void extractFromChatShouldSkipBlankUserMessagesAndReturnEmptyResultSafely() {
        long baseTime = 1_700_003_300_000L;
        seedUser(4L, baseTime);
        seedProject(40L, 4L, baseTime);
        seedChat(400L, 4L, 40L, baseTime);
        seedMessage(4001L, 400L, 40L, 4L, 1, "USER", "", baseTime + 1);
        seedMessage(4002L, 400L, 40L, 4L, 2, "USER", "   ", baseTime + 2);

        ExtractMemoryResponseVO response = memoryExtractionService.extractFromChat(400L, 40L);

        assertThat(response.getScannedMessageCount()).isEqualTo(2);
        assertThat(response.getExtractedCount()).isZero();
        assertThat(response.getMemories()).isEmpty();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM memory_record", Integer.class)).isZero();
        verify(vectorMemoryService, never()).upsertMemory(any(com.example.memory.entity.MemoryRecord.class));
    }

    @Test
    void extractFromChatShouldReturnEmptyResultWhenMessagesContainNoExtractableKeywords() {
        long baseTime = 1_700_003_400_000L;
        seedUser(5L, baseTime);
        seedProject(50L, 5L, baseTime);
        seedChat(500L, 5L, 50L, baseTime);
        seedMessage(5001L, 500L, 50L, 5L, 1, "USER", "今天先讨论接口联调节奏", baseTime + 1);
        seedMessage(5002L, 500L, 50L, 5L, 2, "USER", "这个问题先记录，稍后继续分析", baseTime + 2);

        ExtractMemoryResponseVO response = memoryExtractionService.extractFromChat(500L, 50L);

        assertThat(response.getScannedMessageCount()).isEqualTo(2);
        assertThat(response.getExtractedCount()).isZero();
        assertThat(response.getMemories()).isEmpty();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, summary
                FROM memory_record
                ORDER BY id
                """);
        assertThat(rows).isEmpty();
        verify(vectorMemoryService, never()).upsertMemory(any(com.example.memory.entity.MemoryRecord.class));
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
