package com.example.memory.controller;

import com.example.memory.common.Result;
import com.example.memory.service.HealthService;
import com.example.memory.vo.HealthStatusVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

    private final HealthService healthService;

    @GetMapping("/health")
    public Result<HealthStatusVO> health() {
        return Result.success(healthService.getHealthStatus());
    }
}

