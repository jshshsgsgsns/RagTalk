package com.example.memory.vo.vector;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorMemoryHitVO {

    private Long memoryId;
    private Long userId;
    private Long projectId;
    private Long chatId;
    private String scopeType;
    private String memoryType;
    private Double importance;
    private Long createdAt;
    private Double score;
    private String contentPreview;
}
