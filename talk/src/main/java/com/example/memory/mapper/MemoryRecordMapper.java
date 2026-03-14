package com.example.memory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.memory.entity.MemoryRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemoryRecordMapper extends BaseMapper<MemoryRecord> {
}
