package com.example.memory.vo.memory;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractMemoryResponseVO {

    private Long projectId;
    private Long chatId;
    private Integer scannedMessageCount;
    private Integer extractedCount;
    private List<MemoryRecordVO> memories;
}
