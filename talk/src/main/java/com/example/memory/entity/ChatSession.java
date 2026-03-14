package com.example.memory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("chat_session")
public class ChatSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userAccountId;

    private Long projectSpaceId;

    private String sessionCode;

    private String sessionTitle;

    private String sessionStatus;

    private Long startedAt;

    private Long endedAt;

    private Long lastMessageAt;

    private String metadataJson;

    private Long createdAt;

    private Long updatedAt;
}
