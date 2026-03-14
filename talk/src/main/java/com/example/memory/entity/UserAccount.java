package com.example.memory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_account")
public class UserAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userCode;

    private String displayName;

    private String accountStatus;

    private String profileJson;

    private Long createdAt;

    private Long updatedAt;
}
