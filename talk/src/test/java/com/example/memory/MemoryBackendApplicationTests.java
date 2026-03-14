package com.example.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MemoryBackendApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void healthEndpointShouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.application").value("memory-backend"));
    }

    @Test
    void schemaShouldInitializeRequiredTablesAndIndexes() {
        Integer tableCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM sqlite_master
                WHERE type = 'table'
                  AND name IN ('user_account', 'project_space', 'chat_session',
                               'message_event', 'memory_record', 'conversation_summary')
                """, Integer.class);
        Integer indexCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM sqlite_master
                WHERE type = 'index'
                  AND name IN ('uk_user_account_user_code',
                               'uk_project_space_user_space_code',
                               'uk_chat_session_session_code',
                               'uk_message_event_session_sequence',
                               'uk_conversation_summary_session_version',
                               'idx_memory_record_scope_type')
                """, Integer.class);

        assertThat(tableCount).isEqualTo(6);
        assertThat(indexCount).isEqualTo(6);
    }
}
