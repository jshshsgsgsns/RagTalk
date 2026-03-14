package com.example.memory.vo.vector;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorMemoryUpsertResponseVO {

    private Long memoryId;
    private String documentId;
    private String scopeType;
    private String memoryType;
    private String status;
}
