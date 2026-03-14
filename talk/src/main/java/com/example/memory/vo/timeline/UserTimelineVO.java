package com.example.memory.vo.timeline;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTimelineVO {

    private Long userId;
    private Integer totalEvents;
    private List<TimelineEventVO> events;
}
