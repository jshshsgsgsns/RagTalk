package com.example.memory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("memory_record")
public class MemoryRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userAccountId;

    private Long projectSpaceId;

    private Long sessionId;

    private Long sourceMessageEventId;

    private String memoryScope;

    private String memoryType;

    private String title;

    private String summary;

    private String detailText;

    private String tagsJson;

    private String metadataJson;

    private Double importanceScore;

    private Double confidenceScore;

    private Long lastAccessedAt;

    private Long expiresAt;

    private Long createdAt;

    private Long updatedAt;
}
