package com.example.memory.dto.memory;

import com.example.memory.dto.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MemoryRecordCreateRequest {

    @NotNull(message = "userId must not be null")
    private Long userId;

    @NotNull(message = "projectId must not be null")
    private Long projectId;

    private Long chatId;

    private Long sourceMessageEventId;

    @NotBlank(message = "scopeType must not be blank")
    @Pattern(regexp = ValidationPatterns.MEMORY_SCOPE_TYPE, message = "scopeType must be one of global, project, chat")
    private String scopeType;

    @NotBlank(message = "memoryType must not be blank")
    @Pattern(
            regexp = ValidationPatterns.MEMORY_TYPE,
            message = "memoryType must be one of preference, profile, habit, requirement, constraint, decision, summary, fact")
    private String memoryType;

    private String title;

    @NotBlank(message = "summary must not be blank")
    private String summary;

    private String detailText;

    private String tagsJson;

    private String metadataJson;

    private Double importance;

    private Double confidence;
}
