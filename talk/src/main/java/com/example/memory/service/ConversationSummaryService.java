package com.example.memory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.memory.common.ResultCode;
import com.example.memory.entity.ChatSession;
import com.example.memory.entity.ConversationSummary;
import com.example.memory.entity.MessageEvent;
import com.example.memory.exception.BusinessException;
import com.example.memory.mapper.ChatSessionMapper;
import com.example.memory.mapper.ConversationSummaryMapper;
import com.example.memory.mapper.MessageEventMapper;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ConversationSummaryService {

    private static final String SUMMARY_PROVIDER = "RULE";
    private static final String SUMMARY_MODEL = "rule-based-summary";

    private final ChatSessionMapper chatSessionMapper;
    private final MessageEventMapper messageEventMapper;
    private final ConversationSummaryMapper conversationSummaryMapper;

    @Transactional
    public ConversationSummary generateSummary(Long chatId, Integer sourceStartSequence, Integer sourceEndSequence) {
        if (sourceStartSequence == null || sourceEndSequence == null
                || sourceStartSequence <= 0 || sourceEndSequence <= 0 || sourceStartSequence > sourceEndSequence) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "invalid summary message range");
        }

        ChatSession chatSession = chatSessionMapper.selectById(chatId);
        if (chatSession == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "chat session not found");
        }

        List<MessageEvent> events = messageEventMapper.selectList(
                new LambdaQueryWrapper<MessageEvent>()
                        .eq(MessageEvent::getSessionId, chatId)
                        .ge(MessageEvent::getSequenceNo, sourceStartSequence)
                        .le(MessageEvent::getSequenceNo, sourceEndSequence)
                        .orderByAsc(MessageEvent::getSequenceNo));
        if (events.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "message range not found for summary");
        }

        String summaryText = buildSummaryText(events);
        long now = Instant.now().toEpochMilli();

        ConversationSummary existing = conversationSummaryMapper.selectOne(
                new LambdaQueryWrapper<ConversationSummary>()
                        .eq(ConversationSummary::getSessionId, chatId)
                        .eq(ConversationSummary::getSourceStartSequence, sourceStartSequence)
                        .eq(ConversationSummary::getSourceEndSequence, sourceEndSequence)
                        .last("LIMIT 1"));
        if (existing != null) {
            existing.setSummaryText(summaryText);
            existing.setProvider(SUMMARY_PROVIDER);
            existing.setModelName(SUMMARY_MODEL);
            existing.setMetadataJson(buildMetadataJson(events.size()));
            existing.setUpdatedAt(now);
            conversationSummaryMapper.updateById(existing);
            return existing;
        }

        Integer maxVersion = conversationSummaryMapper.selectList(
                new LambdaQueryWrapper<ConversationSummary>()
                        .eq(ConversationSummary::getSessionId, chatId))
                .stream()
                .map(ConversationSummary::getSummaryVersion)
                .filter(version -> version != null)
                .max(Integer::compareTo)
                .orElse(0);

        ConversationSummary summary = new ConversationSummary();
        summary.setSessionId(chatId);
        summary.setProjectSpaceId(chatSession.getProjectSpaceId());
        summary.setSummaryVersion(maxVersion + 1);
        summary.setSourceStartSequence(sourceStartSequence);
        summary.setSourceEndSequence(sourceEndSequence);
        summary.setSummaryText(summaryText);
        summary.setProvider(SUMMARY_PROVIDER);
        summary.setModelName(SUMMARY_MODEL);
        summary.setMetadataJson(buildMetadataJson(events.size()));
        summary.setCreatedAt(now);
        summary.setUpdatedAt(now);
        conversationSummaryMapper.insert(summary);
        return summary;
    }

    private String buildSummaryText(List<MessageEvent> events) {
        StringBuilder builder = new StringBuilder();
        MessageEvent first = events.get(0);
        MessageEvent last = events.get(events.size() - 1);
        builder.append("Summary(")
                .append(first.getSequenceNo())
                .append('-')
                .append(last.getSequenceNo())
                .append("): ");

        for (int i = 0; i < events.size(); i++) {
            MessageEvent event = events.get(i);
            if (i > 0) {
                builder.append(" | ");
            }
            builder.append('[')
                    .append(event.getSequenceNo())
                    .append("] ")
                    .append(event.getRole())
                    .append(": ")
                    .append(compactContent(event.getContentText()));
        }
        return builder.toString();
    }

    private String compactContent(String contentText) {
        if (!StringUtils.hasText(contentText)) {
            return "(empty)";
        }
        String normalized = contentText.trim().replaceAll("\\s+", " ");
        return normalized.length() <= 80 ? normalized : normalized.substring(0, 77) + "...";
    }

    private String buildMetadataJson(int messageCount) {
        return "{\"messageCount\":" + messageCount + "}";
    }
}
