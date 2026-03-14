package com.example.memory.vo.timeline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineEventVO {

    private String eventType;
    private Long eventTime;
    private Long userId;
    private Long projectId;
    private Long chatId;
    private Long sourceId;
    private String title;
    private String summary;
    private String detail;
}
