package com.example.memory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("project_space")
public class ProjectSpace {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userAccountId;

    private String spaceCode;

    private String spaceName;

    private String memoryScope;

    private String description;

    private String settingsJson;

    private Long createdAt;

    private Long updatedAt;
}
