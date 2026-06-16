package com.aquarium.iot.controller;

import com.aquarium.common.dto.ApiResponse;
import com.aquarium.common.dto.DeviceCommand;
import com.aquarium.common.enums.DeviceType;
import com.aquarium.iot.executor.DeviceCommandExecutor;
import com.aquarium.iot.executor.DeviceExecutorFactory;
import com.aquarium.iot.entity.DeviceCommandLog;
import com.aquarium.iot.mapper.DeviceCommandLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/iot")
@RequiredArgsConstructor
public class IotController {

    private final DeviceExecutorFactory executorFactory;
    private final DeviceCommandLogMapper commandLogMapper;

    @PostMapping("/command")
    public ApiResponse<String> sendCommand(@RequestBody Map<String, Object> body) {
        String tankId = (String) body.get("tankId");
        String deviceTypeCode = (String) body.get("deviceType");
        String action = (String) body.get("action");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) body.getOrDefault("params", Map.of());

        DeviceCommand command = DeviceCommand.builder()
                .commandId(UUID.randomUUID().toString())
                .tankId(tankId)
                .deviceType(DeviceType.fromCode(deviceTypeCode))
                .action(action)
                .params(params)
                .issuedAt(LocalDateTime.now())
                .build();

        DeviceCommandExecutor executor = executorFactory.getExecutor(command.getDeviceType());
        String result = executor.execute(command);

        return ApiResponse.success(result);
    }

    @GetMapping("/commands/tank/{tankId}")
    public ApiResponse<List<DeviceCommandLog>> getCommandHistory(@PathVariable String tankId) {
        List<DeviceCommandLog> logs = commandLogMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DeviceCommandLog>()
                        .eq(DeviceCommandLog::getTankId, tankId)
                        .orderByDesc(DeviceCommandLog::getIssuedAt)
                        .last("LIMIT 50"));
        return ApiResponse.success(logs);
    }
}
