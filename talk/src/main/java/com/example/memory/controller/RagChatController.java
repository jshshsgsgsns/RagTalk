package com.example.memory.controller;

import com.example.memory.common.Result;
import com.example.memory.dto.chat.RagChatSendRequest;
import com.example.memory.service.RagChatService;
import com.example.memory.vo.chat.RagChatResponseVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class RagChatController {

    private final RagChatService ragChatService;

    @PostMapping("/rag-send")
    public Result<RagChatResponseVO> send(@Valid @RequestBody RagChatSendRequest request) {
        return Result.success(ragChatService.send(request));
    }
}
