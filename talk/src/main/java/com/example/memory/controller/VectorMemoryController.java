package com.example.memory.controller;

import com.example.memory.common.Result;
import com.example.memory.dto.vector.VectorMemorySearchRequest;
import com.example.memory.service.VectorMemoryService;
import com.example.memory.vo.vector.VectorMemorySearchResponseVO;
import com.example.memory.vo.vector.VectorMemoryUpsertResponseVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/vector/memory")
@RequiredArgsConstructor
public class VectorMemoryController {

    private final VectorMemoryService vectorMemoryService;

    @PostMapping("/upsert/{memoryId}")
    public Result<VectorMemoryUpsertResponseVO> upsert(
            @PathVariable @Positive(message = "memoryId must be greater than 0") Long memoryId) {
        return Result.success(vectorMemoryService.upsertMemory(memoryId));
    }

    @PostMapping("/search")
    public Result<VectorMemorySearchResponseVO> search(@Valid @RequestBody VectorMemorySearchRequest request) {
        return Result.success(vectorMemoryService.searchMemories(request));
    }
}
