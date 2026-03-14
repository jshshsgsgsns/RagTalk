package com.example.memory.dto;

public final class ValidationPatterns {

    public static final String MEMORY_SCOPE_TYPE =
            "^(?i)(global|project|chat)$";

    public static final String MEMORY_TYPE =
            "^(?i)(preference|profile|habit|requirement|constraint|decision|summary|fact)$";

    private ValidationPatterns() {
    }
}
