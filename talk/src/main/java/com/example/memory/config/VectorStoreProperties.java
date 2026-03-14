package com.example.memory.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.vector.qdrant")
public class VectorStoreProperties {

    private boolean enabled = false;

    @NotBlank
    private String host = "localhost";

    @Min(1)
    private int grpcPort = 6334;

    private boolean useTls = false;

    private String apiKey;

    @NotBlank
    private String collectionName = "memory_record_vectors";

    private boolean initializeSchema = true;

    @Min(1)
    private int searchTopKDefault = 5;

    @Min(1)
    private int timeoutSeconds = 5;
}
