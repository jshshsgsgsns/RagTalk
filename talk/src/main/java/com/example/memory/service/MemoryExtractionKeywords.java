package com.example.memory.service;

import java.util.List;

public final class MemoryExtractionKeywords {

    public static final List<String> DECISION = List.of(
            "\u51b3\u5b9a",
            "\u6700\u7ec8",
            "\u786e\u5b9a",
            "\u786e\u8ba4",
            "\u5b9a\u4e3a");
    public static final List<String> CONSTRAINT = List.of(
            "\u5fc5\u987b",
            "\u4e0d\u5141\u8bb8",
            "\u53ea\u80fd",
            "\u7981\u6b62");
    public static final List<String> REQUIREMENT = List.of(
            "\u9700\u8981",
            "\u8981\u6c42");
    public static final List<String> HABIT = List.of(
            "\u4e60\u60ef",
            "\u4ee5\u540e",
            "\u901a\u5e38",
            "\u4e00\u76f4",
            "\u5e38\u5e38");
    public static final List<String> PREFERENCE = List.of(
            "\u559c\u6b22",
            "\u504f\u597d");
    public static final List<String> PROFILE = List.of(
            "\u6211\u662f",
            "\u6211\u53eb",
            "\u6211\u7684\u5de5\u4f5c",
            "\u6211\u7684\u8eab\u4efd");

    public static final List<String> GLOBAL_SCOPE = List.of(
            "\u4ee5\u540e",
            "\u957f\u671f",
            "\u4e00\u76f4",
            "\u901a\u5e38",
            "\u5e38\u5e38",
            "\u4e60\u60ef");
    public static final List<String> PROJECT_SCOPE = List.of(
            "\u8fd9\u4e2a\u9879\u76ee",
            "\u672c\u9879\u76ee",
            "\u9879\u76ee\u91cc",
            "\u9879\u76ee\u4e2d",
            "\u9700\u8981",
            "\u8981\u6c42",
            "\u5fc5\u987b",
            "\u4e0d\u5141\u8bb8",
            "\u53ea\u80fd",
            "\u7981\u6b62",
            "\u51b3\u5b9a",
            "\u6700\u7ec8",
            "\u786e\u5b9a",
            "\u786e\u8ba4",
            "\u5b9a\u4e3a");

    private MemoryExtractionKeywords() {
    }
}
