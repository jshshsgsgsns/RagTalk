package com.example.memory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.memory.common.ResultCode;
import com.example.memory.config.MemoryExtractionProperties;
import com.example.memory.entity.ChatSession;
import com.example.memory.entity.MemoryRecord;
import com.example.memory.entity.MessageEvent;
import com.example.memory.exception.BusinessException;
import com.example.memory.mapper.ChatSessionMapper;
import com.example.memory.mapper.MessageEventMapper;
import com.example.memory.vo.memory.ExtractMemoryResponseVO;
import com.example.memory.vo.memory.MemoryRecordVO;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class MemoryExtractionService {

    private final ChatSessionMapper chatSessionMapper;
    private final MessageEventMapper messageEventMapper;
    private final MemoryRecordService memoryRecordService;
    private final MemoryExtractionProperties memoryExtractionProperties;

    @Transactional
    public ExtractMemoryResponseVO extractFromChat(Long chatId, Long projectId) {
        ChatSession chatSession = chatSessionMapper.selectById(chatId);
        if (chatSession == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "chat session not found");
        }
        if (!projectId.equals(chatSession.getProjectSpaceId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "chat session does not belong to project space");
        }

        List<MessageEvent> events = messageEventMapper.selectList(
                new LambdaQueryWrapper<MessageEvent>()
                        .eq(MessageEvent::getSessionId, chatId)
                        .eq(MessageEvent::getRole, "USER")
                        .orderByDesc(MessageEvent::getSequenceNo)
                        .last("LIMIT " + memoryExtractionProperties.getRecentMessageLimit()));
        events.sort(Comparator.comparing(MessageEvent::getSequenceNo));

        List<MemoryRecordVO> extractedMemories = new ArrayList<>();
        for (MessageEvent event : events) {
            ExtractionCandidate candidate = extractCandidate(event, chatSession);
            if (candidate == null) {
                continue;
            }
            MemoryRecord savedRecord = memoryRecordService.saveOrUpdateExtractedMemory(candidate.memoryRecord());
            extractedMemories.add(memoryRecordService.toVO(savedRecord));
        }

        return ExtractMemoryResponseVO.builder()
                .projectId(projectId)
                .chatId(chatId)
                .scannedMessageCount(events.size())
                .extractedCount(extractedMemories.size())
                .memories(extractedMemories)
                .build();
    }

    private ExtractionCandidate extractCandidate(MessageEvent event, ChatSession chatSession) {
        if (!StringUtils.hasText(event.getContentText())) {
            return null;
        }

        String text = event.getContentText().trim();
        String memoryType = inferMemoryType(text);
        if (memoryType == null) {
            return null;
        }

        String scope = inferScope(text);
        MemoryRecord record = new MemoryRecord();
        record.setUserAccountId(chatSession.getUserAccountId());
        record.setProjectSpaceId(chatSession.getProjectSpaceId());
        record.setSessionId(chatSession.getId());
        record.setSourceMessageEventId(event.getId());
        record.setMemoryScope(scope);
        record.setMemoryType(memoryType);
        record.setTitle(buildTitle(memoryType, scope));
        record.setSummary(text);
        record.setDetailText(text);
        record.setImportanceScore(inferImportance(memoryType, scope));
        record.setConfidenceScore(0.7D);
        return new ExtractionCandidate(record);
    }

    private String inferMemoryType(String text) {
        if (containsAny(text, MemoryExtractionKeywords.DECISION)) {
            return MemoryRecordService.TYPE_DECISION;
        }
        if (containsAny(text, MemoryExtractionKeywords.CONSTRAINT)) {
            return MemoryRecordService.TYPE_CONSTRAINT;
        }
        if (containsAny(text, MemoryExtractionKeywords.REQUIREMENT)) {
            return MemoryRecordService.TYPE_REQUIREMENT;
        }
        if (containsAny(text, MemoryExtractionKeywords.HABIT)) {
            return MemoryRecordService.TYPE_HABIT;
        }
        if (containsAny(text, MemoryExtractionKeywords.PREFERENCE)) {
            return MemoryRecordService.TYPE_PREFERENCE;
        }
        if (containsAny(text, MemoryExtractionKeywords.PROFILE)) {
            return MemoryRecordService.TYPE_PROFILE;
        }
        return null;
    }

    private String inferScope(String text) {
        if (containsAny(text, MemoryExtractionKeywords.GLOBAL_SCOPE)) {
            return MemoryRecordService.SCOPE_GLOBAL;
        }
        if (containsAny(text, MemoryExtractionKeywords.PROJECT_SCOPE)) {
            return MemoryRecordService.SCOPE_PROJECT;
        }
        return MemoryRecordService.SCOPE_CHAT;
    }

    private String buildTitle(String memoryType, String scope) {
        return scope + ":" + memoryType;
    }

    private Double inferImportance(String memoryType, String scope) {
        if (MemoryRecordService.TYPE_DECISION.equals(memoryType) || MemoryRecordService.TYPE_CONSTRAINT.equals(memoryType)) {
            return 0.95D;
        }
        if (MemoryRecordService.SCOPE_GLOBAL.equals(scope)) {
            return 0.88D;
        }
        if (MemoryRecordService.TYPE_REQUIREMENT.equals(memoryType)) {
            return 0.9D;
        }
        return 0.75D;
    }

    private boolean containsAny(String text, Iterable<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private record ExtractionCandidate(MemoryRecord memoryRecord) {
    }
}
