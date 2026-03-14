package com.example.memory.service;

import com.example.memory.config.RagChatProperties;
import com.example.memory.entity.MessageEvent;
import com.example.memory.vo.vector.RagMemorySearchResult;
import com.example.memory.vo.vector.VectorMemoryHitVO;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class RagPromptBuilder {

    private final RagChatProperties ragChatProperties;

    public List<Message> buildPromptMessages(
            List<MessageEvent> recentEvents,
            String latestUserMessage,
            RagMemorySearchResult memorySearchResult) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(buildSystemPrompt(memorySearchResult)));

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

        if (messages.isEmpty()
                || !(messages.get(messages.size() - 1) instanceof UserMessage userMessage)
                || !latestUserMessage.equals(userMessage.getText())) {
            messages.add(new UserMessage(latestUserMessage));
        }
        return messages;
    }

    private String buildSystemPrompt(RagMemorySearchResult memorySearchResult) {
        StringBuilder builder = new StringBuilder();
        builder.append(ragChatProperties.getSystemPrompt()).append('\n');
        builder.append("Retrieved memory context follows. Prefer current project memories. ");
        builder.append("Use chat memories as local supplements. ");
        builder.append("Use global preference/profile/habit as supplemental context only.\n");
        builder.append("Do not apply old project requirement/constraint/decision memories to this chat.\n\n");
        builder.append(renderSection("Project memories", memorySearchResult.getProjectMemories()));
        builder.append(renderSection("Chat memories", memorySearchResult.getChatMemories()));
        builder.append(renderSection("Global memories", memorySearchResult.getGlobalMemories()));
        return builder.toString();
    }

    private String renderSection(String title, List<VectorMemoryHitVO> hits) {
        StringBuilder builder = new StringBuilder();
        builder.append(title).append(":\n");
        if (hits == null || hits.isEmpty()) {
            builder.append("- none\n\n");
            return builder.toString();
        }
        for (VectorMemoryHitVO hit : hits) {
            builder.append("- [memoryId=")
                    .append(hit.getMemoryId())
                    .append(", scope=")
                    .append(hit.getScopeType())
                    .append(", type=")
                    .append(hit.getMemoryType())
                    .append(", importance=")
                    .append(hit.getImportance())
                    .append("] ")
                    .append(hit.getContentPreview())
                    .append('\n');
        }
        builder.append('\n');
        return builder.toString();
    }
}
