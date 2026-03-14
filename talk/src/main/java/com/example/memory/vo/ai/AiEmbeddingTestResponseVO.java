package com.example.memory.vo.ai;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiEmbeddingTestResponseVO {

    private String provider;
    private String model;
    private String text;
    private Integer dimensions;
    private List<Float> vectorPreview;
}
