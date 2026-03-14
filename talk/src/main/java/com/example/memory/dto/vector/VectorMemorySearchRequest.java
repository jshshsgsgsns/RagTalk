package com.example.memory.dto.vector;

import com.example.memory.dto.ValidationPatterns;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.Data;

@Data
public class VectorMemorySearchRequest {

    @NotNull(message = "userId must not be null")
    private Long userId;

    @NotNull(message = "projectId must not be null")
    private Long projectId;

    private Long chatId;

    private List<@Pattern(
            regexp = ValidationPatterns.MEMORY_SCOPE_TYPE,
            message = "scopeTypes must contain only global, project, chat") String> scopeTypes;

    @NotBlank(message = "queryText must not be blank")
    private String queryText;

    @Min(value = 1, message = "topK must be greater than 0")
    private Integer topK;
}
