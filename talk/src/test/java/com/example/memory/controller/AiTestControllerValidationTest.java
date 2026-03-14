package com.example.memory.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.memory.ai.AiFacade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.vector.qdrant.enabled=false")
@AutoConfigureMockMvc
class AiTestControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiFacade aiFacade;

    @Test
    void chatTestShouldRejectBlankMessage() throws Exception {
        mockMvc.perform(post("/api/ai/chat/test")
                        .contentType("application/json")
                        .content("""
                                {
                                  "systemPrompt": "test",
                                  "message": "   "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(422))
                .andExpect(jsonPath("$.message").value("message: message must not be blank"));
    }

    @Test
    void embeddingTestShouldRejectBlankText() throws Exception {
        mockMvc.perform(post("/api/ai/embedding/test")
                        .contentType("application/json")
                        .content("""
                                {
                                  "text": ""
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(422))
                .andExpect(jsonPath("$.message").value("text: text must not be blank"));
    }
}
