package com.aquarium.common.enums;

import lombok.Getter;

@Getter
public enum WaterMetric {
    TEMPERATURE("temperature", "水温", "℃"),
    PH("ph", "pH值", ""),
    CHLORINE("chlorine", "残余氯气", "mg/L"),
    DISSOLVED_OXYGEN("dissolved_oxygen", "溶氧量", "mg/L");

    private final String code;
    private final String name;
    private final String unit;

    WaterMetric(String code, String name, String unit) {
        this.code = code;
        this.name = name;
        this.unit = unit;
    }

    public static WaterMetric fromCode(String code) {
        for (WaterMetric metric : values()) {
            if (metric.code.equals(code)) {
                return metric;
            }
        }
        throw new IllegalArgumentException("Unknown water metric: " + code);
    }
}
