package com.example.memory.service;

import com.example.memory.config.AiProperties;
import com.example.memory.config.RagChatProperties;
import com.example.memory.dto.chat.RagChatSendRequest;
import com.example.memory.entity.ChatSession;
import com.example.memory.entity.MessageEvent;
import com.example.memory.entity.ProjectSpace;
import com.example.memory.vo.chat.RagChatResponseVO;
import com.example.memory.vo.chat.RagMemorySectionVO;
import com.example.memory.vo.vector.RagMemorySearchResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RagChatService {

    private final ChatSessionSupportService chatSessionSupportService;
    private final VectorMemoryService vectorMemoryService;
    private final RagPromptBuilder ragPromptBuilder;
    private final ChatClient chatClient;
    private final RagChatProperties ragChatProperties;
    private final AiProperties aiProperties;

    @Transactional
    public RagChatResponseVO send(RagChatSendRequest request) {
        ProjectSpace projectSpace = chatSessionSupportService.requireProjectSpace(request.getProjectId());
        ChatSession chatSession = chatSessionSupportService.requireChatSession(request.getChatId());
        chatSessionSupportService.validateSessionOwnership(projectSpace, chatSession);

        long now = Instant.now().toEpochMilli();
        int nextSequenceNo = chatSessionSupportService.nextSequenceNo(chatSession.getId());
        MessageEvent userEvent = chatSessionSupportService.saveMessage(
                chatSession,
                nextSequenceNo,
                ChatSessionSupportService.ROLE_USER,
                request.getMessage(),
                now,
                null);

        RagMemorySearchResult memorySearchResult = vectorMemoryService.searchForRag(
                chatSession.getUserAccountId(),
                chatSession.getProjectSpaceId(),
                chatSession.getId(),
                request.getMessage(),
                ragChatProperties.getMemoryTopK());

        List<MessageEvent> recentEvents = chatSessionSupportService.listRecentEvents(
                chatSession.getId(),
                ragChatProperties.getHistoryMessageLimit());
        List<Message> promptMessages = ragPromptBuilder.buildPromptMessages(recentEvents, request.getMessage(), memorySearchResult);

        String assistantReply = chatClient.prompt()
                .messages(promptMessages)
                .call()
                .content();

        long replyTime = Instant.now().toEpochMilli();
        MessageEvent assistantEvent = chatSessionSupportService.saveMessage(
                chatSession,
                nextSequenceNo + 1,
                ChatSessionSupportService.ROLE_ASSISTANT,
                assistantReply,
                replyTime,
                Map.of(
                        "provider", aiProperties.getChat().getProvider().name(),
                        "model", aiProperties.getChat().getModel(),
                        "ragMemoryCount", memorySearchResult.total()));

        chatSessionSupportService.updateSessionTimestamps(chatSession.getId(), replyTime);

        return RagChatResponseVO.builder()
                .projectId(projectSpace.getId())
                .chatId(chatSession.getId())
                .userMessageId(userEvent.getId())
                .assistantMessageId(assistantEvent.getId())
                .assistantReply(assistantReply)
                .usedHistoryMessageCount(recentEvents.size())
                .usedMemoryCount(memorySearchResult.total())
                .userSequenceNo(userEvent.getSequenceNo())
                .assistantSequenceNo(assistantEvent.getSequenceNo())
                .provider(aiProperties.getChat().getProvider().name())
                .model(aiProperties.getChat().getModel())
                .retrievedMemories(RagMemorySectionVO.builder()
                        .total(memorySearchResult.total())
                        .projectMemories(memorySearchResult.getProjectMemories())
                        .chatMemories(memorySearchResult.getChatMemories())
                        .globalMemories(memorySearchResult.getGlobalMemories())
                        .build())
                .build();
    }
}
