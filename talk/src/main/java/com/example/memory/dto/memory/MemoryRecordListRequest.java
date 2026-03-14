package com.example.memory.dto.memory;

import com.example.memory.dto.ValidationPatterns;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MemoryRecordListRequest {

    private Long userId;

    private Long projectId;

    private Long chatId;

    @Pattern(regexp = ValidationPatterns.MEMORY_SCOPE_TYPE, message = "scopeType must be one of global, project, chat")
    private String scopeType;

    @Pattern(
            regexp = ValidationPatterns.MEMORY_TYPE,
            message = "memoryType must be one of preference, profile, habit, requirement, constraint, decision, summary, fact")
    private String memoryType;
}
