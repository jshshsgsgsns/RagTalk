package com.example.memory.controller;

import com.example.memory.common.Result;
import com.example.memory.dto.chat.ChatSendRequest;
import com.example.memory.service.ChatOrchestrationService;
import com.example.memory.vo.chat.ChatSendResponseVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatOrchestrationService chatOrchestrationService;

    @PostMapping("/send")
    public Result<ChatSendResponseVO> send(@Valid @RequestBody ChatSendRequest request) {
        return Result.success(chatOrchestrationService.send(request));
    }
}
