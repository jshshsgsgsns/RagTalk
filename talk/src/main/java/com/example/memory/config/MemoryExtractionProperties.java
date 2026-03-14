package com.example.memory.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.memory.extraction")
public class MemoryExtractionProperties {

    @Min(1)
    private int recentMessageLimit = 20;
}
