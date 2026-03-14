package com.example.memory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("conversation_summary")
public class ConversationSummary {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private Long projectSpaceId;

    private Integer summaryVersion;

    private Integer sourceStartSequence;

    private Integer sourceEndSequence;

    private String summaryText;

    private String provider;

    private String modelName;

    private String metadataJson;

    private Long createdAt;

    private Long updatedAt;
}
