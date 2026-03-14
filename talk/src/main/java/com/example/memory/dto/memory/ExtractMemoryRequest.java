package com.example.memory.dto.memory;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExtractMemoryRequest {

    @NotNull(message = "projectId must not be null")
    private Long projectId;
}
