package com.example.memory.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.memory.exception.BusinessException;
import com.example.memory.service.VectorMemoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.vector.qdrant.enabled=false")
@AutoConfigureMockMvc
class VectorMemoryControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VectorMemoryService vectorMemoryService;

    @Test
    void searchShouldRejectBlankQueryText() throws Exception {
        mockMvc.perform(post("/api/vector/memory/search")
                        .contentType("application/json")
                        .content("""
                                {
                                  "userId": 1,
                                  "projectId": 10,
                                  "queryText": "   ",
                                  "topK": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(422))
                .andExpect(jsonPath("$.message").value("queryText: queryText must not be blank"));
    }

    @Test
    void searchShouldRejectNonPositiveTopK() throws Exception {
        mockMvc.perform(post("/api/vector/memory/search")
                        .contentType("application/json")
                        .content("""
                                {
                                  "userId": 1,
                                  "projectId": 10,
                                  "queryText": "find memory",
                                  "topK": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(422))
                .andExpect(jsonPath("$.message").value("topK: topK must be greater than 0"));
    }

    @Test
    void searchShouldRejectInvalidScopeTypeElement() throws Exception {
        mockMvc.perform(post("/api/vector/memory/search")
                        .contentType("application/json")
                        .content("""
                                {
                                  "userId": 1,
                                  "projectId": 10,
                                  "queryText": "find memory",
                                  "topK": 2,
                                  "scopeTypes": ["project", "workspace"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(422))
                .andExpect(jsonPath("$.message").value("scopeTypes[1]: scopeTypes must contain only global, project, chat"));
    }

    @Test
    void upsertShouldReturnBusinessErrorWhenMemoryDoesNotExist() throws Exception {
        given(vectorMemoryService.upsertMemory(999L))
                .willThrow(new BusinessException(400, "memory record not found"));

        mockMvc.perform(post("/api/vector/memory/upsert/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("memory record not found"));
    }
}
