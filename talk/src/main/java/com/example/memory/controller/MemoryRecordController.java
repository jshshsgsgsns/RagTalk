package com.example.memory.controller;

import com.example.memory.common.Result;
import com.example.memory.dto.memory.ExtractMemoryRequest;
import com.example.memory.dto.memory.MemoryRecordCreateRequest;
import com.example.memory.dto.memory.MemoryRecordListRequest;
import com.example.memory.service.MemoryExtractionService;
import com.example.memory.service.MemoryRecordService;
import com.example.memory.vo.memory.ExtractMemoryResponseVO;
import com.example.memory.vo.memory.MemoryRecordVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
public class MemoryRecordController {

    private final MemoryRecordService memoryRecordService;
    private final MemoryExtractionService memoryExtractionService;

    @PostMapping("/api/memory-records")
    public Result<MemoryRecordVO> create(@Valid @RequestBody MemoryRecordCreateRequest request) {
        return Result.success(memoryRecordService.create(request));
    }

    @GetMapping("/api/memory-records/{memoryId}")
    public Result<MemoryRecordVO> detail(@PathVariable @Positive(message = "memoryId must be greater than 0") Long memoryId) {
        return Result.success(memoryRecordService.detail(memoryId));
    }

    @GetMapping("/api/memory-records")
    public Result<List<MemoryRecordVO>> list(@Valid MemoryRecordListRequest request) {
        return Result.success(memoryRecordService.list(request));
    }

    @PostMapping("/api/chat/{chatId}/extract-memory")
    public Result<ExtractMemoryResponseVO> extractMemory(
            @PathVariable @Positive(message = "chatId must be greater than 0") Long chatId,
            @Valid @RequestBody ExtractMemoryRequest request) {
        return Result.success(memoryExtractionService.extractFromChat(chatId, request.getProjectId()));
    }
}
