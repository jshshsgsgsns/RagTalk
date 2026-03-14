package com.example.memory.ai;

import com.example.memory.config.AiProperties;
import com.example.memory.vo.ai.AiChatTestResponseVO;
import com.example.memory.vo.ai.AiEmbeddingTestResponseVO;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AiFacade {

    private static final int EMBEDDING_PREVIEW_SIZE = 8;

    @Qualifier("chatModel")
    private final ChatModel chatModel;

    @Qualifier("embeddingModel")
    private final EmbeddingModel embeddingModel;

    private final AiProperties aiProperties;

    public AiChatTestResponseVO chat(String userMessage, String systemPrompt) {
        List<Message> messages = new ArrayList<>();
        if (StringUtils.hasText(systemPrompt)) {
            messages.add(new SystemMessage(systemPrompt));
        }
        messages.add(new UserMessage(userMessage));

        String answer = chatModel.call(new Prompt(messages))
                .getResult()
                .getOutput()
                .getText();

        return AiChatTestResponseVO.builder()
                .provider(aiProperties.getChat().getProvider().name())
                .model(aiProperties.getChat().getModel())
                .message(userMessage)
                .reply(answer)
                .build();
    }

    public AiEmbeddingTestResponseVO embed(String text) {
        float[] embedding = embeddingModel.embed(text);
        int previewLength = Math.min(embedding.length, EMBEDDING_PREVIEW_SIZE);
        List<Float> preview = new ArrayList<>(previewLength);
        for (int index = 0; index < previewLength; index++) {
            preview.add(embedding[index]);
        }

        return AiEmbeddingTestResponseVO.builder()
                .provider(aiProperties.getEmbedding().getProvider().name())
                .model(aiProperties.getEmbedding().getModel())
                .text(text)
                .dimensions(embedding.length)
                .vectorPreview(preview)
                .build();
    }
}
