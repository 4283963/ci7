package com.aquarium.common.enums;

import lombok.Getter;

@Getter
public enum AlertLevel {
    INFO("info", "提示"),
    WARNING("warning", "警告"),
    CRITICAL("critical", "严重"),
    FATAL("fatal", "致命");

    private final String code;
    private final String description;

    AlertLevel(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
