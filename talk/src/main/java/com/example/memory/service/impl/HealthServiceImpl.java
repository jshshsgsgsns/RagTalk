package com.example.memory.service.impl;

import com.example.memory.service.HealthService;
import com.example.memory.vo.HealthStatusVO;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class HealthServiceImpl implements HealthService {

    @Value("${spring.application.name:memory-backend}")
    private String applicationName;

    @Override
    public HealthStatusVO getHealthStatus() {
        return HealthStatusVO.builder()
                .status("UP")
                .application(applicationName)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

