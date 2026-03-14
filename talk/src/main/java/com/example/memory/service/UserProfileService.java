package com.example.memory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.memory.entity.MemoryRecord;
import com.example.memory.mapper.MemoryRecordMapper;
import com.example.memory.vo.profile.UserProfileVO;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final MemoryRecordMapper memoryRecordMapper;

    public UserProfileVO buildProfile(Long userId) {
        List<MemoryRecord> globalMemories = memoryRecordMapper.selectList(
                new LambdaQueryWrapper<MemoryRecord>()
                        .eq(MemoryRecord::getUserAccountId, userId)
                        .eq(MemoryRecord::getMemoryScope, MemoryRecordService.SCOPE_GLOBAL)
                        .orderByDesc(MemoryRecord::getImportanceScore)
                        .orderByDesc(MemoryRecord::getUpdatedAt));

        Set<String> preferences = new LinkedHashSet<>();
        Set<String> habits = new LinkedHashSet<>();
        Set<String> projectDirections = new LinkedHashSet<>();
        Set<String> techPreferences = new LinkedHashSet<>();

        for (MemoryRecord memory : globalMemories) {
            String content = normalize(memory.getDetailText());
            if (!StringUtils.hasText(content)) {
                content = normalize(memory.getSummary());
            }

            if (MemoryRecordService.TYPE_PREFERENCE.equals(memory.getMemoryType())) {
                preferences.add(content);
            }
            if (MemoryRecordService.TYPE_HABIT.equals(memory.getMemoryType())) {
                habits.add(content);
            }
            if (MemoryRecordService.TYPE_PROFILE.equals(memory.getMemoryType())
                    || containsAny(content, "项目", "产品", "平台", "系统", "管理", "内容")) {
                projectDirections.add(content);
            }
            if (containsAny(content.toLowerCase(Locale.ROOT), "java", "spring", "react", "vue", "python", "mysql", "sqlite", "qdrant")) {
                techPreferences.add(content);
            }
        }

        return UserProfileVO.builder()
                .userId(userId)
                .preferences(preferences.stream().toList())
                .longTermHabits(habits.stream().toList())
                .commonProjectDirections(projectDirections.stream().toList())
                .commonTechPreferences(techPreferences.stream().toList())
                .build();
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
