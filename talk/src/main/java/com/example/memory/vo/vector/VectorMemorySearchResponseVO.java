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
public class VectorMemorySearchResponseVO {

    private String queryText;
    private Integer topK;
    private Integer totalHits;
    private Integer projectHits;
    private Integer supplementalHits;
    private List<VectorMemoryHitVO> hits;
}
