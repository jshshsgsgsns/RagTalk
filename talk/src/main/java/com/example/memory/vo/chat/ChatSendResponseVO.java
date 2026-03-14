package com.example.memory.vo.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSendResponseVO {

    private Long projectId;
    private Long chatId;
    private Long userMessageId;
    private Long assistantMessageId;
    private String assistantReply;
    private Integer usedContextMessageCount;
    private Integer userSequenceNo;
    private Integer assistantSequenceNo;
    private String provider;
    private String model;
}
