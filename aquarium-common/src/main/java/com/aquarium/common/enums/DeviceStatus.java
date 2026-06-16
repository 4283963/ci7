package com.aquarium.common.enums;

import lombok.Getter;

@Getter
public enum DeviceStatus {
    ON("on", "开启"),
    OFF("off", "关闭"),
    ERROR("error", "故障"),
    OFFLINE("offline", "离线");

    private final String code;
    private final String description;

    DeviceStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
