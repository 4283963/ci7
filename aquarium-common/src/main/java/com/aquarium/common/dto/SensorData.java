package com.aquarium.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorData {
    private String deviceId;
    private Double temperature;
    private Double ph;
    private Double chlorine;
    private Double dissolvedOxygen;
    private LocalDateTime timestamp;
}
