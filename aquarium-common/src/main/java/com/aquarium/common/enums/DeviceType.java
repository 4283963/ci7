package com.aquarium.common.enums;

import lombok.Getter;

@Getter
public enum DeviceType {
    HEATER("heater", "加温棒"),
    CHLORINE_VALVE("chlorine_valve", "排氯补水阀"),
    WATER_PUMP("water_pump", "水泵"),
    AERATION_PUMP("aeration_pump", "增氧泵");

    private final String code;
    private final String description;

    DeviceType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static DeviceType fromCode(String code) {
        for (DeviceType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown device type: " + code);
    }
}
