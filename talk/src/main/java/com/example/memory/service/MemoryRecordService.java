package com.example.memory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.memory.common.ResultCode;
import com.example.memory.dto.memory.MemoryRecordCreateRequest;
import com.example.memory.dto.memory.MemoryRecordListRequest;
import com.example.memory.entity.ChatSession;
import com.example.memory.entity.MemoryRecord;
import com.example.memory.entity.ProjectSpace;
import com.example.memory.exception.BusinessException;
import com.example.memory.mapper.ChatSessionMapper;
import com.example.memory.mapper.MemoryRecordMapper;
import com.example.memory.mapper.ProjectSpaceMapper;
import com.example.memory.vo.memory.MemoryRecordVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class MemoryRecordService {

    public static final String SCOPE_GLOBAL = "global";
    public static final String SCOPE_PROJECT = "project";
    public static final String SCOPE_CHAT = "chat";

    public static final String TYPE_PREFERENCE = "preference";
    public static final String TYPE_PROFILE = "profile";
    public static final String TYPE_HABIT = "habit";
    public static final String TYPE_REQUIREMENT = "requirement";
    public static final String TYPE_CONSTRAINT = "constraint";
    public static final String TYPE_DECISION = "decision";
    public static final String TYPE_SUMMARY = "summary";
    public static final String TYPE_FACT = "fact";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final MemoryRecordMapper memoryRecordMapper;
    private final ProjectSpaceMapper projectSpaceMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final VectorMemoryService vectorMemoryService;

    @Transactional
    public MemoryRecordVO create(MemoryRecordCreateRequest request) {
        validateScope(request.getScopeType());
        validateMemoryType(request.getMemoryType());
        validateOwnership(request.getProjectId(), request.getChatId());

        long now = Instant.now().toEpochMilli();
        MemoryRecord record = new MemoryRecord();
        record.setUserAccountId(request.getUserId());
        record.setProjectSpaceId(request.getProjectId());
        record.setSessionId(request.getChatId());
        record.setSourceMessageEventId(request.getSourceMessageEventId());
        record.setMemoryScope(normalizeScope(request.getScopeType()));
        record.setMemoryType(normalizeMemoryType(request.getMemoryType()));
        record.setTitle(request.getTitle());
        record.setSummary(request.getSummary().trim());
        record.setDetailText(request.getDetailText());
        record.setTagsJson(request.getTagsJson());
        record.setMetadataJson(request.getMetadataJson());
        record.setImportanceScore(defaultIfNull(request.getImportance(), 0.7D));
        record.setConfidenceScore(defaultIfNull(request.getConfidence(), 0.8D));
        record.setCreatedAt(now);
        record.setUpdatedAt(now);

        memoryRecordMapper.insert(record);
        vectorMemoryService.upsertMemory(record);
        return toVO(record);
    }

    public MemoryRecordVO detail(Long memoryId) {
        return toVO(requireMemory(memoryId));
    }

    public List<MemoryRecordVO> list(MemoryRecordListRequest request) {
        LambdaQueryWrapper<MemoryRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(request.getUserId() != null, MemoryRecord::getUserAccountId, request.getUserId())
                .eq(request.getProjectId() != null, MemoryRecord::getProjectSpaceId, request.getProjectId())
                .eq(request.getChatId() != null, MemoryRecord::getSessionId, request.getChatId())
                .eq(StringUtils.hasText(request.getScopeType()), MemoryRecord::getMemoryScope, normalizeScope(request.getScopeType()))
                .eq(StringUtils.hasText(request.getMemoryType()), MemoryRecord::getMemoryType, normalizeMemoryType(request.getMemoryType()))
                .orderByDesc(MemoryRecord::getUpdatedAt)
                .orderByDesc(MemoryRecord::getId);
        return memoryRecordMapper.selectList(queryWrapper).stream()
                .map(this::toVO)
                .toList();
    }

    @Transactional
    public MemoryRecord saveOrUpdateExtractedMemory(MemoryRecord record) {
        validateScope(record.getMemoryScope());
        validateMemoryType(record.getMemoryType());

        String normalizedScope = normalizeScope(record.getMemoryScope());
        String normalizedMemoryType = normalizeMemoryType(record.getMemoryType());
        String memoryKey = resolveMemoryKey(record);

        record.setMemoryScope(normalizedScope);
        record.setMemoryType(normalizedMemoryType);

        MemoryRecord existing = findExistingRecord(record, memoryKey);
        long now = Instant.now().toEpochMilli();
        if (existing == null) {
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            memoryRecordMapper.insert(record);
            vectorMemoryService.upsertMemory(record);
            return record;
        }

        existing.setUserAccountId(record.getUserAccountId());
        existing.setProjectSpaceId(record.getProjectSpaceId());
        existing.setSessionId(record.getSessionId());
        existing.setSourceMessageEventId(record.getSourceMessageEventId());
        existing.setMemoryScope(record.getMemoryScope());
        existing.setMemoryType(record.getMemoryType());
        existing.setTitle(preferIncomingText(record.getTitle(), existing.getTitle()));
        existing.setSummary(preferIncomingText(record.getSummary(), existing.getSummary()));
        existing.setDetailText(preferIncomingText(record.getDetailText(), existing.getDetailText()));
        existing.setTagsJson(preferIncomingText(record.getTagsJson(), existing.getTagsJson()));
        existing.setMetadataJson(preferIncomingText(record.getMetadataJson(), existing.getMetadataJson()));
        existing.setImportanceScore(maxScore(existing.getImportanceScore(), record.getImportanceScore()));
        existing.setConfidenceScore(maxScore(existing.getConfidenceScore(), record.getConfidenceScore()));
        existing.setUpdatedAt(now);
        memoryRecordMapper.updateById(existing);
        vectorMemoryService.upsertMemory(existing);
        return existing;
    }

    public MemoryRecord requireMemory(Long memoryId) {
        MemoryRecord record = memoryRecordMapper.selectById(memoryId);
        if (record == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "memory record not found");
        }
        return record;
    }

    public MemoryRecordVO toVO(MemoryRecord record) {
        return MemoryRecordVO.builder()
                .id(record.getId())
                .userId(record.getUserAccountId())
                .projectId(record.getProjectSpaceId())
                .chatId(record.getSessionId())
                .sourceMessageEventId(record.getSourceMessageEventId())
                .scopeType(record.getMemoryScope())
                .memoryType(record.getMemoryType())
                .title(record.getTitle())
                .summary(record.getSummary())
                .detailText(record.getDetailText())
                .importance(record.getImportanceScore())
                .confidence(record.getConfidenceScore())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }

    private MemoryRecord findExistingRecord(MemoryRecord record, String memoryKey) {
        LambdaQueryWrapper<MemoryRecord> queryWrapper = new LambdaQueryWrapper<MemoryRecord>()
                .eq(MemoryRecord::getUserAccountId, record.getUserAccountId())
                .eq(MemoryRecord::getMemoryScope, record.getMemoryScope())
                .eq(MemoryRecord::getMemoryType, record.getMemoryType());

        if (SCOPE_PROJECT.equals(record.getMemoryScope())) {
            queryWrapper.eq(MemoryRecord::getProjectSpaceId, record.getProjectSpaceId());
        }
        else if (SCOPE_CHAT.equals(record.getMemoryScope())) {
            queryWrapper.eq(MemoryRecord::getSessionId, record.getSessionId());
        }

        return memoryRecordMapper.selectList(queryWrapper).stream()
                .filter(existing -> memoryKey.equals(resolveMemoryKey(existing)))
                .max(Comparator.comparing(MemoryRecord::getUpdatedAt, Comparator.nullsLast(Long::compareTo))
                        .thenComparing(MemoryRecord::getId, Comparator.nullsLast(Long::compareTo)))
                .orElse(null);
    }

    private void validateOwnership(Long projectId, Long chatId) {
        ProjectSpace projectSpace = projectSpaceMapper.selectById(projectId);
        if (projectSpace == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "project space not found");
        }
        if (chatId == null) {
            return;
        }
        ChatSession chatSession = chatSessionMapper.selectById(chatId);
        if (chatSession == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "chat session not found");
        }
        if (!projectId.equals(chatSession.getProjectSpaceId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "chat session does not belong to project space");
        }
    }

    private void validateScope(String scope) {
        String normalized = normalizeScope(scope);
        if (!SCOPE_GLOBAL.equals(normalized) && !SCOPE_PROJECT.equals(normalized) && !SCOPE_CHAT.equals(normalized)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "unsupported scope type");
        }
    }

    private void validateMemoryType(String memoryType) {
        String normalized = normalizeMemoryType(memoryType);
        if (!List.of(TYPE_PREFERENCE, TYPE_PROFILE, TYPE_HABIT, TYPE_REQUIREMENT,
                        TYPE_CONSTRAINT, TYPE_DECISION, TYPE_SUMMARY, TYPE_FACT).contains(normalized)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "unsupported memory type");
        }
    }

    private String normalizeScope(String scope) {
        return scope == null ? null : scope.trim().toLowerCase();
    }

    private String normalizeMemoryType(String memoryType) {
        return memoryType == null ? null : memoryType.trim().toLowerCase();
    }

    private Double defaultIfNull(Double value, Double defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String resolveMemoryKey(MemoryRecord record) {
        String explicitKey = extractMemoryKey(record.getMetadataJson());
        if (StringUtils.hasText(explicitKey)) {
            return normalizeMemoryKey(explicitKey);
        }
        if (StringUtils.hasText(record.getSummary())) {
            return normalizeMemoryKey(record.getSummary());
        }
        if (StringUtils.hasText(record.getDetailText())) {
            return normalizeMemoryKey(record.getDetailText());
        }
        return normalizeMemoryKey(record.getTitle());
    }

    private String extractMemoryKey(String metadataJson) {
        if (!StringUtils.hasText(metadataJson)) {
            return null;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(metadataJson);
            JsonNode memoryKeyNode = root.get("memoryKey");
            return memoryKeyNode == null || memoryKeyNode.isNull() ? null : memoryKeyNode.asText();
        }
        catch (Exception exception) {
            return null;
        }
    }

    private String normalizeMemoryKey(String rawMemoryKey) {
        if (!StringUtils.hasText(rawMemoryKey)) {
            return "";
        }
        return rawMemoryKey.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private String preferIncomingText(String incoming, String existing) {
        return StringUtils.hasText(incoming) ? incoming.trim() : existing;
    }

    private Double maxScore(Double existing, Double incoming) {
        if (existing == null) {
            return incoming;
        }
        if (incoming == null) {
            return existing;
        }
        return Math.max(existing, incoming);
    }
}
