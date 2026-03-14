package com.example.memory.vo.vector;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagMemorySearchResult {

    private List<VectorMemoryHitVO> projectMemories;
    private List<VectorMemoryHitVO> chatMemories;
    private List<VectorMemoryHitVO> globalMemories;

    public int total() {
        return size(projectMemories) + size(chatMemories) + size(globalMemories);
    }

    private int size(List<VectorMemoryHitVO> items) {
        return items == null ? 0 : items.size();
    }
}
