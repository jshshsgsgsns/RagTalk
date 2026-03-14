package com.example.memory.vo.chat;

import com.example.memory.vo.vector.VectorMemoryHitVO;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagMemorySectionVO {

    private Integer total;
    private List<VectorMemoryHitVO> projectMemories;
    private List<VectorMemoryHitVO> chatMemories;
    private List<VectorMemoryHitVO> globalMemories;
}
