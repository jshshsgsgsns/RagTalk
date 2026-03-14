package com.example.memory.controller;

import com.example.memory.common.Result;
import com.example.memory.service.UserProfileService;
import com.example.memory.service.UserTimelineService;
import com.example.memory.vo.profile.UserProfileVO;
import com.example.memory.vo.timeline.UserTimelineVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserInsightController {

    private final UserProfileService userProfileService;
    private final UserTimelineService userTimelineService;

    @GetMapping("/api/profile/{userId}")
    public Result<UserProfileVO> profile(@PathVariable Long userId) {
        return Result.success(userProfileService.buildProfile(userId));
    }

    @GetMapping("/api/timeline/{userId}")
    public Result<UserTimelineVO> timeline(@PathVariable Long userId) {
        return Result.success(userTimelineService.buildTimeline(userId));
    }
}
