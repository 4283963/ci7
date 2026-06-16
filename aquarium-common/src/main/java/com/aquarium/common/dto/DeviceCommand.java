package com.aquarium.common.dto;

import com.aquarium.common.enums.DeviceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceCommand {
    private String commandId;
    private String tankId;
    private DeviceType deviceType;
    private String action;
    private Map<String, Object> params;
    private LocalDateTime issuedAt;
}
