package com.example.memory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.memory.entity.ChatSession;
import com.example.memory.entity.ConversationSummary;
import com.example.memory.entity.MemoryRecord;
import com.example.memory.entity.MessageEvent;
import com.example.memory.entity.ProjectSpace;
import com.example.memory.mapper.ChatSessionMapper;
import com.example.memory.mapper.ConversationSummaryMapper;
import com.example.memory.mapper.MemoryRecordMapper;
import com.example.memory.mapper.MessageEventMapper;
import com.example.memory.mapper.ProjectSpaceMapper;
import com.example.memory.vo.timeline.TimelineEventVO;
import com.example.memory.vo.timeline.UserTimelineVO;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserTimelineService {

    private final ProjectSpaceMapper projectSpaceMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final MessageEventMapper messageEventMapper;
    private final MemoryRecordMapper memoryRecordMapper;
    private final ConversationSummaryMapper conversationSummaryMapper;

    public UserTimelineVO buildTimeline(Long userId) {
        List<TimelineEventVO> events = new ArrayList<>();

        List<ProjectSpace> projectSpaces = projectSpaceMapper.selectList(
                new LambdaQueryWrapper<ProjectSpace>().eq(ProjectSpace::getUserAccountId, userId));
        for (ProjectSpace projectSpace : projectSpaces) {
            events.add(TimelineEventVO.builder()
                    .eventType("PROJECT_SPACE")
                    .eventTime(projectSpace.getCreatedAt())
                    .userId(userId)
                    .projectId(projectSpace.getId())
                    .sourceId(projectSpace.getId())
                    .title(projectSpace.getSpaceName())
                    .summary(projectSpace.getDescription())
                    .detail(projectSpace.getMemoryScope())
                    .build());
        }

        List<ChatSession> chatSessions = chatSessionMapper.selectList(
                new LambdaQueryWrapper<ChatSession>().eq(ChatSession::getUserAccountId, userId));
        for (ChatSession chatSession : chatSessions) {
            events.add(TimelineEventVO.builder()
                    .eventType("CHAT_SESSION")
                    .eventTime(chatSession.getStartedAt())
                    .userId(userId)
                    .projectId(chatSession.getProjectSpaceId())
                    .chatId(chatSession.getId())
                    .sourceId(chatSession.getId())
                    .title(chatSession.getSessionTitle())
                    .summary(chatSession.getSessionStatus())
                    .detail(chatSession.getSessionCode())
                    .build());
        }

        List<MessageEvent> messageEvents = messageEventMapper.selectList(
                new LambdaQueryWrapper<MessageEvent>().eq(MessageEvent::getUserAccountId, userId));
        for (MessageEvent messageEvent : messageEvents) {
            events.add(TimelineEventVO.builder()
                    .eventType("MESSAGE_EVENT")
                    .eventTime(messageEvent.getEventTime())
                    .userId(userId)
                    .projectId(messageEvent.getProjectSpaceId())
                    .chatId(messageEvent.getSessionId())
                    .sourceId(messageEvent.getId())
                    .title(messageEvent.getRole())
                    .summary(messageEvent.getContentText())
                    .detail(messageEvent.getEventType())
                    .build());
        }

        List<MemoryRecord> memoryRecords = memoryRecordMapper.selectList(
                new LambdaQueryWrapper<MemoryRecord>().eq(MemoryRecord::getUserAccountId, userId));
        for (MemoryRecord memoryRecord : memoryRecords) {
            events.add(TimelineEventVO.builder()
                    .eventType("MEMORY_RECORD")
                    .eventTime(memoryRecord.getCreatedAt())
                    .userId(userId)
                    .projectId(memoryRecord.getProjectSpaceId())
                    .chatId(memoryRecord.getSessionId())
                    .sourceId(memoryRecord.getId())
                    .title(memoryRecord.getMemoryType())
                    .summary(memoryRecord.getSummary())
                    .detail(memoryRecord.getMemoryScope())
                    .build());
        }

        List<Long> projectIds = projectSpaces.stream().map(ProjectSpace::getId).toList();
        if (!projectIds.isEmpty()) {
            List<ConversationSummary> summaries = conversationSummaryMapper.selectList(
                    new LambdaQueryWrapper<ConversationSummary>()
                            .in(ConversationSummary::getProjectSpaceId, projectIds));
            for (ConversationSummary summary : summaries) {
                events.add(TimelineEventVO.builder()
                        .eventType("CONVERSATION_SUMMARY")
                        .eventTime(summary.getCreatedAt())
                        .userId(userId)
                        .projectId(summary.getProjectSpaceId())
                        .chatId(summary.getSessionId())
                        .sourceId(summary.getId())
                        .title("summary-v" + summary.getSummaryVersion())
                        .summary(summary.getSummaryText())
                        .detail(summary.getModelName())
                        .build());
            }
        }

        events.sort(Comparator.comparing(TimelineEventVO::getEventTime, Comparator.nullsLast(Long::compareTo)).reversed());
        return UserTimelineVO.builder()
                .userId(userId)
                .totalEvents(events.size())
                .events(events)
                .build();
    }
}
