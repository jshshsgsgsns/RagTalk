package com.example.memory.vo.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatTestResponseVO {

    private String provider;
    private String model;
    private String message;
    private String reply;
}
