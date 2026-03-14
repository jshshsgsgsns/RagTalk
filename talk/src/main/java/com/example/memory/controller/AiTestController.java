package com.example.memory.controller;

import com.example.memory.ai.AiFacade;
import com.example.memory.common.Result;
import com.example.memory.dto.ai.AiChatTestRequest;
import com.example.memory.dto.ai.AiEmbeddingTestRequest;
import com.example.memory.vo.ai.AiChatTestResponseVO;
import com.example.memory.vo.ai.AiEmbeddingTestResponseVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiTestController {

    private final AiFacade aiFacade;

    @PostMapping("/chat/test")
    public Result<AiChatTestResponseVO> chatTest(@Valid @RequestBody AiChatTestRequest request) {
        return Result.success(aiFacade.chat(request.getMessage(), request.getSystemPrompt()));
    }

    @PostMapping("/embedding/test")
    public Result<AiEmbeddingTestResponseVO> embeddingTest(@Valid @RequestBody AiEmbeddingTestRequest request) {
        return Result.success(aiFacade.embed(request.getText()));
    }
}
