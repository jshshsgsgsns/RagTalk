package com.example.memory.service;

import com.example.memory.config.AiProperties;
import com.example.memory.config.ChatFlowProperties;
import com.example.memory.dto.chat.ChatSendRequest;
import com.example.memory.entity.ChatSession;
import com.example.memory.entity.MessageEvent;
import com.example.memory.entity.ProjectSpace;
import com.example.memory.vo.chat.ChatSendResponseVO;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ChatOrchestrationService {

    private final ChatSessionSupportService chatSessionSupportService;
    private final ChatClient chatClient;
    private final ChatFlowProperties chatFlowProperties;
    private final AiProperties aiProperties;

    @Transactional
    public ChatSendResponseVO send(ChatSendRequest request) {
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

        List<MessageEvent> recentEvents = chatSessionSupportService.listRecentEvents(
                chatSession.getId(),
                chatFlowProperties.getContextMessageLimit());
        List<Message> promptMessages = buildPromptMessages(recentEvents, request.getMessage());

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
                        "model", aiProperties.getChat().getModel()));

        chatSessionSupportService.updateSessionTimestamps(chatSession.getId(), replyTime);

        return ChatSendResponseVO.builder()
                .projectId(projectSpace.getId())
                .chatId(chatSession.getId())
                .userMessageId(userEvent.getId())
                .assistantMessageId(assistantEvent.getId())
                .assistantReply(assistantReply)
                .usedContextMessageCount(recentEvents.size())
                .userSequenceNo(userEvent.getSequenceNo())
                .assistantSequenceNo(assistantEvent.getSequenceNo())
                .provider(aiProperties.getChat().getProvider().name())
                .model(aiProperties.getChat().getModel())
                .build();
    }

    private List<Message> buildPromptMessages(List<MessageEvent> recentEvents, String latestUserMessage) {
        List<Message> messages = new ArrayList<>();
        if (StringUtils.hasText(chatFlowProperties.getDefaultSystemPrompt())) {
            messages.add(new SystemMessage(chatFlowProperties.getDefaultSystemPrompt()));
        }
        for (MessageEvent event : recentEvents) {
            if (!StringUtils.hasText(event.getContentText())) {
                continue;
            }
            if (ChatSessionSupportService.ROLE_USER.equals(event.getRole())) {
                messages.add(new UserMessage(event.getContentText()));
            }
            else if (ChatSessionSupportService.ROLE_ASSISTANT.equals(event.getRole())) {
                messages.add(new AssistantMessage(event.getContentText()));
            }
        }

        if (messages.isEmpty() || !(messages.get(messages.size() - 1) instanceof UserMessage userMessage)
                || !latestUserMessage.equals(userMessage.getText())) {
            messages.add(new UserMessage(latestUserMessage));
        }
        return messages;
    }
}
