package com.example.memory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.memory.common.ResultCode;
import com.example.memory.config.AiProperties;
import com.example.memory.entity.ChatSession;
import com.example.memory.entity.MessageEvent;
import com.example.memory.entity.ProjectSpace;
import com.example.memory.exception.BusinessException;
import com.example.memory.mapper.ChatSessionMapper;
import com.example.memory.mapper.MessageEventMapper;
import com.example.memory.mapper.ProjectSpaceMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatSessionSupportService {

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ASSISTANT = "ASSISTANT";
    public static final String EVENT_TYPE_MESSAGE = "MESSAGE";

    private final ProjectSpaceMapper projectSpaceMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final MessageEventMapper messageEventMapper;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    public ProjectSpace requireProjectSpace(Long projectId) {
        ProjectSpace projectSpace = projectSpaceMapper.selectById(projectId);
        if (projectSpace == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "project space not found");
        }
        return projectSpace;
    }

    public ChatSession requireChatSession(Long chatId) {
        ChatSession chatSession = chatSessionMapper.selectById(chatId);
        if (chatSession == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "chat session not found");
        }
        return chatSession;
    }

    public void validateSessionOwnership(ProjectSpace projectSpace, ChatSession chatSession) {
        if (!projectSpace.getId().equals(chatSession.getProjectSpaceId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "chat session does not belong to project space");
        }
    }

    public int nextSequenceNo(Long sessionId) {
        MessageEvent latestEvent = messageEventMapper.selectOne(
                new LambdaQueryWrapper<MessageEvent>()
                        .eq(MessageEvent::getSessionId, sessionId)
                        .orderByDesc(MessageEvent::getSequenceNo)
                        .last("LIMIT 1"));
        return latestEvent == null ? 1 : latestEvent.getSequenceNo() + 1;
    }

    public List<MessageEvent> listRecentEvents(Long sessionId, int limit) {
        List<MessageEvent> events = messageEventMapper.selectList(
                new LambdaQueryWrapper<MessageEvent>()
                        .eq(MessageEvent::getSessionId, sessionId)
                        .orderByDesc(MessageEvent::getSequenceNo)
                        .last("LIMIT " + limit));
        events.sort(Comparator.comparing(MessageEvent::getSequenceNo));
        return events;
    }

    public MessageEvent saveMessage(
            ChatSession chatSession,
            int sequenceNo,
            String role,
            String contentText,
            long eventTime,
            Map<String, Object> extraMetadata) {
        MessageEvent event = buildMessageEvent(chatSession, sequenceNo, role, contentText, eventTime, extraMetadata);
        messageEventMapper.insert(event);
        return event;
    }

    public void updateSessionTimestamps(Long chatId, long updatedAt) {
        chatSessionMapper.update(
                null,
                new LambdaUpdateWrapper<ChatSession>()
                        .eq(ChatSession::getId, chatId)
                        .set(ChatSession::getUpdatedAt, updatedAt)
                        .set(ChatSession::getLastMessageAt, updatedAt));
    }

    private MessageEvent buildMessageEvent(
            ChatSession chatSession,
            int sequenceNo,
            String role,
            String contentText,
            long eventTime,
            Map<String, Object> extraMetadata) {
        MessageEvent event = new MessageEvent();
        event.setSessionId(chatSession.getId());
        event.setProjectSpaceId(chatSession.getProjectSpaceId());
        event.setUserAccountId(chatSession.getUserAccountId());
        event.setSequenceNo(sequenceNo);
        event.setRole(role);
        event.setEventType(EVENT_TYPE_MESSAGE);
        event.setContentText(contentText);
        event.setContentJson(toEventJson(chatSession, sequenceNo, role, contentText, eventTime, extraMetadata));
        event.setProvider(aiProperties.getChat().getProvider().name());
        event.setModelName(aiProperties.getChat().getModel());
        event.setEventTime(eventTime);
        event.setCreatedAt(eventTime);
        return event;
    }

    private String toEventJson(
            ChatSession chatSession,
            int sequenceNo,
            String role,
            String contentText,
            long eventTime,
            Map<String, Object> extraMetadata) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", chatSession.getId());
        payload.put("projectSpaceId", chatSession.getProjectSpaceId());
        payload.put("userAccountId", chatSession.getUserAccountId());
        payload.put("sequenceNo", sequenceNo);
        payload.put("role", role);
        payload.put("eventType", EVENT_TYPE_MESSAGE);
        payload.put("contentText", contentText);
        payload.put("eventTime", eventTime);
        if (extraMetadata != null && !extraMetadata.isEmpty()) {
            payload.put("metadata", extraMetadata);
        }
        try {
            return objectMapper.writeValueAsString(payload);
        }
        catch (JsonProcessingException exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "failed to serialize message event");
        }
    }
}
