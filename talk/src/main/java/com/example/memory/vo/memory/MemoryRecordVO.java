package com.example.memory.vo.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryRecordVO {

    private Long id;
    private Long userId;
    private Long projectId;
    private Long chatId;
    private Long sourceMessageEventId;
    private String scopeType;
    private String memoryType;
    private String title;
    private String summary;
    private String detailText;
    private Double importance;
    private Double confidence;
    private Long createdAt;
    private Long updatedAt;
}
