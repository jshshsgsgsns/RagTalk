package com.example.memory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("message_event")
public class MessageEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private Long projectSpaceId;

    private Long userAccountId;

    private Integer sequenceNo;

    private String role;

    private String eventType;

    private String contentText;

    private String contentJson;

    private String provider;

    private String modelName;

    private String requestId;

    private Integer tokenUsageInput;

    private Integer tokenUsageOutput;

    private Long eventTime;

    private Long createdAt;
}
