package com.example.memory.vo.profile;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileVO {

    private Long userId;
    private List<String> preferences;
    private List<String> longTermHabits;
    private List<String> commonProjectDirections;
    private List<String> commonTechPreferences;
}
