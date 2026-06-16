package com.aquarium.common.dto;

import com.aquarium.common.enums.AlertLevel;
import com.aquarium.common.enums.WaterMetric;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterQualityAlert {
    private String alertId;
    private String tankId;
    private WaterMetric metric;
    private Double actualValue;
    private Double thresholdMin;
    private Double thresholdMax;
    private AlertLevel level;
    private String message;
    private LocalDateTime createdAt;
}
