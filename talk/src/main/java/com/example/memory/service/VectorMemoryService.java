package com.example.memory.service;

import com.example.memory.common.ResultCode;
import com.example.memory.config.VectorStoreProperties;
import com.example.memory.dto.vector.VectorMemorySearchRequest;
import com.example.memory.entity.MemoryRecord;
import com.example.memory.exception.BusinessException;
import com.example.memory.mapper.MemoryRecordMapper;
import com.example.memory.vo.vector.RagMemorySearchResult;
import com.example.memory.vo.vector.VectorMemoryHitVO;
import com.example.memory.vo.vector.VectorMemorySearchResponseVO;
import com.example.memory.vo.vector.VectorMemoryUpsertResponseVO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class VectorMemoryService {

    private static final String SCOPE_GLOBAL = "global";
    private static final String SCOPE_USER = "user";

    private final MemoryRecordMapper memoryRecordMapper;
    private final VectorStore memoryVectorStore;
    private final VectorStoreProperties vectorStoreProperties;

    public VectorMemoryUpsertResponseVO upsertMemory(Long memoryId) {
        MemoryRecord memoryRecord = requireMemoryRecord(memoryId);
        upsertMemory(memoryRecord);
        return VectorMemoryUpsertResponseVO.builder()
                .memoryId(memoryRecord.getId())
                .documentId(memoryRecord.getId().toString())
                .scopeType(memoryRecord.getMemoryScope())
                .memoryType(memoryRecord.getMemoryType())
                .status("UPSERTED")
                .build();
    }

    public void upsertMemory(MemoryRecord memoryRecord) {
        memoryVectorStore.add(List.of(toDocument(memoryRecord)));
    }

    public void deleteMemory(Long memoryId) {
        memoryVectorStore.delete(memoryId.toString());
    }

    public VectorMemorySearchResponseVO searchMemories(
            Long userId,
            Long projectId,
            Long chatId,
            List<String> scopeTypes,
            String queryText,
            Integer topK) {
        int limit = topK == null ? vectorStoreProperties.getSearchTopKDefault() : topK;
        List<String> normalizedScopes = normalizeScopeTypes(scopeTypes);
        Map<Long, VectorMemoryHitVO> mergedHits = new LinkedHashMap<>();

        if (chatId != null) {
            collectHits(mergedHits, searchStage(queryText, limit, currentChatFilter(userId, projectId, chatId, normalizedScopes)));
        }
        int projectTargetSize = mergedHits.size() < limit ? limit : mergedHits.size();
        collectHits(mergedHits, searchStage(queryText, projectTargetSize, currentProjectFilter(userId, projectId, normalizedScopes)));

        int projectHits = mergedHits.size();
        List<String> supplementalScopes = normalizedScopes.stream()
                .filter(this::isSupplementalScope)
                .toList();
        if (mergedHits.size() < limit && !supplementalScopes.isEmpty()) {
            collectHits(
                    mergedHits,
                    searchStage(queryText, limit - mergedHits.size(), supplementalFilter(userId, projectId, supplementalScopes)));
        }

        List<VectorMemoryHitVO> finalHits = mergedHits.values().stream()
                .limit(limit)
                .toList();

        return VectorMemorySearchResponseVO.builder()
                .queryText(queryText)
                .topK(limit)
                .totalHits(finalHits.size())
                .projectHits(Math.min(projectHits, finalHits.size()))
                .supplementalHits(Math.max(0, finalHits.size() - Math.min(projectHits, finalHits.size())))
                .hits(finalHits)
                .build();
    }

    public VectorMemorySearchResponseVO searchMemories(VectorMemorySearchRequest request) {
        return searchMemories(
                request.getUserId(),
                request.getProjectId(),
                request.getChatId(),
                request.getScopeTypes(),
                request.getQueryText(),
                request.getTopK());
    }

    public RagMemorySearchResult searchForRag(Long userId, Long projectId, Long chatId, String queryText, Integer topK) {
        int limit = topK == null ? vectorStoreProperties.getSearchTopKDefault() : topK;
        List<VectorMemoryHitVO> projectMemories = searchStage(
                queryText,
                limit,
                scopedFilter(userId, projectId, null, MemoryRecordService.SCOPE_PROJECT, null));
        List<VectorMemoryHitVO> chatMemories = searchStage(
                queryText,
                limit,
                scopedFilter(userId, projectId, chatId, MemoryRecordService.SCOPE_CHAT, null));
        List<VectorMemoryHitVO> globalMemories = searchStage(
                queryText,
                limit,
                scopedFilter(
                        userId,
                        null,
                        null,
                        MemoryRecordService.SCOPE_GLOBAL,
                        List.of(
                                MemoryRecordService.TYPE_PREFERENCE,
                                MemoryRecordService.TYPE_PROFILE,
                                MemoryRecordService.TYPE_HABIT)));

        return RagMemorySearchResult.builder()
                .projectMemories(projectMemories.stream().limit(limit).toList())
                .chatMemories(chatMemories.stream().limit(limit).toList())
                .globalMemories(globalMemories.stream().limit(limit).toList())
                .build();
    }

    private MemoryRecord requireMemoryRecord(Long memoryId) {
        MemoryRecord memoryRecord = memoryRecordMapper.selectById(memoryId);
        if (memoryRecord == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "memory record not found");
        }
        return memoryRecord;
    }

    private Document toDocument(MemoryRecord memoryRecord) {
        return new Document(
                buildVectorText(memoryRecord),
                memoryRecord.getId().toString(),
                buildMetadata(memoryRecord));
    }

    private String buildVectorText(MemoryRecord memoryRecord) {
        StringBuilder builder = new StringBuilder();
        appendLine(builder, memoryRecord.getTitle());
        appendLine(builder, memoryRecord.getSummary());
        appendLine(builder, memoryRecord.getDetailText());
        return builder.toString().trim();
    }

    private void appendLine(StringBuilder builder, String content) {
        if (StringUtils.hasText(content)) {
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(content.trim());
        }
    }

    private Map<String, Object> buildMetadata(MemoryRecord memoryRecord) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("userId", memoryRecord.getUserAccountId());
        metadata.put("projectId", memoryRecord.getProjectSpaceId());
        metadata.put("chatId", memoryRecord.getSessionId() == null ? 0L : memoryRecord.getSessionId());
        metadata.put("memoryId", memoryRecord.getId());
        metadata.put("scopeType", memoryRecord.getMemoryScope());
        metadata.put("memoryType", memoryRecord.getMemoryType());
        metadata.put("importance", memoryRecord.getImportanceScore());
        metadata.put("createdAt", memoryRecord.getCreatedAt());
        return metadata;
    }

    private List<VectorMemoryHitVO> searchStage(String queryText, int topK, Filter.Expression filterExpression) {
        return memoryVectorStore.similaritySearch(SearchRequest.builder()
                        .query(queryText)
                        .topK(topK)
                        .similarityThresholdAll()
                        .filterExpression(filterExpression)
                        .build()).stream()
                .map(this::toHit)
                .toList();
    }

    private void collectHits(Map<Long, VectorMemoryHitVO> mergedHits, List<VectorMemoryHitVO> hits) {
        for (VectorMemoryHitVO hit : hits) {
            mergedHits.putIfAbsent(hit.getMemoryId(), hit);
        }
    }

    private Filter.Expression currentChatFilter(Long userId, Long projectId, Long chatId, List<String> scopeTypes) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op filter = builder.eq("userId", userId);
        filter = builder.and(filter, builder.eq("projectId", projectId));
        filter = builder.and(filter, builder.eq("chatId", chatId));
        if (!scopeTypes.isEmpty()) {
            filter = builder.and(filter, builder.in("scopeType", scopeTypes.stream().map(String.class::cast).toList()));
        }
        return filter.build();
    }

    private Filter.Expression currentProjectFilter(Long userId, Long projectId, List<String> scopeTypes) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op filter = builder.eq("userId", userId);
        filter = builder.and(filter, builder.eq("projectId", projectId));
        if (!scopeTypes.isEmpty()) {
            filter = builder.and(filter, builder.in("scopeType", scopeTypes.stream().map(String.class::cast).toList()));
        }
        return filter.build();
    }

    private Filter.Expression supplementalFilter(Long userId, Long projectId, List<String> scopeTypes) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op filter = builder.eq("userId", userId);
        filter = builder.and(filter, builder.ne("projectId", projectId));
        filter = builder.and(filter, builder.in("scopeType", scopeTypes.stream().map(String.class::cast).toList()));
        return filter.build();
    }

    private Filter.Expression scopedFilter(
            Long userId,
            Long projectId,
            Long chatId,
            String scopeType,
            List<String> memoryTypes) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op filter = builder.eq("userId", userId);
        if (projectId != null) {
            filter = builder.and(filter, builder.eq("projectId", projectId));
        }
        if (chatId != null) {
            filter = builder.and(filter, builder.eq("chatId", chatId));
        }
        filter = builder.and(filter, builder.eq("scopeType", scopeType));
        if (memoryTypes != null && !memoryTypes.isEmpty()) {
            filter = builder.and(filter, builder.in("memoryType", memoryTypes.stream().map(String.class::cast).toList()));
        }
        return filter.build();
    }

    private List<String> normalizeScopeTypes(List<String> scopeTypes) {
        if (scopeTypes == null || scopeTypes.isEmpty()) {
            return List.of();
        }
        Set<String> values = new LinkedHashSet<>();
        for (String scopeType : scopeTypes) {
            if (StringUtils.hasText(scopeType)) {
                values.add(scopeType.trim().toLowerCase());
            }
        }
        return new ArrayList<>(values);
    }

    private boolean isSupplementalScope(String scopeType) {
        String normalized = scopeType == null ? "" : scopeType.trim().toLowerCase();
        return SCOPE_GLOBAL.equals(normalized) || SCOPE_USER.equals(normalized) || normalized.startsWith("global_");
    }

    private VectorMemoryHitVO toHit(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        return VectorMemoryHitVO.builder()
                .memoryId(asLong(metadata.get("memoryId")))
                .userId(asLong(metadata.get("userId")))
                .projectId(asLong(metadata.get("projectId")))
                .chatId(asLong(metadata.get("chatId")))
                .scopeType(asString(metadata.get("scopeType")))
                .memoryType(asString(metadata.get("memoryType")))
                .importance(asDouble(metadata.get("importance")))
                .createdAt(asLong(metadata.get("createdAt")))
                .score(document.getScore())
                .contentPreview(document.getText())
                .build();
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? null : Long.parseLong(String.valueOf(value));
    }

    private Double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return value == null ? null : Double.parseDouble(String.valueOf(value));
    }
}
