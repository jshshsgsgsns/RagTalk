package com.example.memory.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.memory.exception.BusinessException;
import com.example.memory.service.MemoryExtractionService;
import com.example.memory.service.MemoryRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.vector.qdrant.enabled=false")
@AutoConfigureMockMvc
class MemoryRecordControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemoryRecordService memoryRecordService;

    @MockBean
    private MemoryExtractionService memoryExtractionService;

    @Test
    void createShouldRejectMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/memory-records")
                        .contentType("application/json")
                        .content("""
                                {
                                  "projectId": 10,
                                  "scopeType": "project",
                                  "memoryType": "requirement",
                                  "summary": "valid summary"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(422))
                .andExpect(jsonPath("$.message").value("userId: userId must not be null"));
    }

    @Test
    void createShouldRejectInvalidMemoryType() throws Exception {
        mockMvc.perform(post("/api/memory-records")
                        .contentType("application/json")
                        .content("""
                                {
                                  "userId": 1,
                                  "projectId": 10,
                                  "scopeType": "project",
                                  "memoryType": "unsupported",
                                  "summary": "invalid memory type"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(422))
                .andExpect(jsonPath("$.message").value(
                        "memoryType: memoryType must be one of preference, profile, habit, requirement, constraint, decision, summary, fact"));
    }

    @Test
    void createShouldRejectInvalidScopeType() throws Exception {
        mockMvc.perform(post("/api/memory-records")
                        .contentType("application/json")
                        .content("""
                                {
                                  "userId": 1,
                                  "projectId": 10,
                                  "scopeType": "workspace",
                                  "memoryType": "requirement",
                                  "summary": "invalid scope type"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(422))
                .andExpect(jsonPath("$.message").value("scopeType: scopeType must be one of global, project, chat"));
    }

    @Test
    void listShouldRejectInvalidScopeType() throws Exception {
        mockMvc.perform(get("/api/memory-records")
                        .param("scopeType", "workspace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(422))
                .andExpect(jsonPath("$.message").value("scopeType: scopeType must be one of global, project, chat"));
    }

    @Test
    void detailShouldReturnBusinessErrorWhenMemoryDoesNotExist() throws Exception {
        given(memoryRecordService.detail(999L))
                .willThrow(new BusinessException(400, "memory record not found"));

        mockMvc.perform(get("/api/memory-records/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("memory record not found"));
    }
}
