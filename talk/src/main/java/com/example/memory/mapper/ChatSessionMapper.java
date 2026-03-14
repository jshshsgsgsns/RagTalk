package com.example.memory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.memory.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}
