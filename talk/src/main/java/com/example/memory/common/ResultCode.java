package com.example.memory.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResultCode {

    SUCCESS(200, "success"),
    BAD_REQUEST(400, "bad request"),
    VALIDATION_ERROR(422, "validation failed"),
    INTERNAL_ERROR(500, "internal server error");

    private final int code;
    private final String message;
}

