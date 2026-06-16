package com.aquarium.iot.controller;

import com.aquarium.common.dto.ApiResponse;
import com.aquarium.common.dto.DeviceCommand;
import com.aquarium.common.enums.DeviceType;
import com.aquarium.iot.circuit.CircuitBreakerRegistry;
import com.aquarium.iot.executor.DeviceCommandExecutor;
import com.aquarium.iot.executor.DeviceExecutorFactory;
import com.aquarium.iot.entity.DeviceCommandLog;
import com.aquarium.iot.mapper.DeviceCommandLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/iot")
@RequiredArgsConstructor
public class IotController {

    private final DeviceExecutorFactory executorFactory;
    private final DeviceCommandLogMapper commandLogMapper;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @PostMapping("/command")
    public ResponseEntity<ApiResponse<String>> sendCommand(@RequestBody Map<String, Object> body) {
        String tankId = (String) body.get("tankId");
        String deviceTypeCode = (String) body.get("deviceType");
        String action = (String) body.get("action");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) body.getOrDefault("params", Map.of());

        DeviceType deviceType = DeviceType.fromCode(deviceTypeCode);
        var breaker = circuitBreakerRegistry.get("device:" + deviceType.getCode());

        if (!breaker.allowRequest()) {
            log.warn("IOT command rejected: circuit breaker open for {}", deviceTypeCode);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error(503,
                            "Device " + deviceTypeCode + " temporarily unavailable, please try again later"));
        }

        DeviceCommand command = DeviceCommand.builder()
                .commandId(UUID.randomUUID().toString())
                .tankId(tankId)
                .deviceType(deviceType)
                .action(action)
                .params(params)
                .issuedAt(LocalDateTime.now())
                .build();

        String result;
        try {
            DeviceCommandExecutor executor = executorFactory.getExecutor(deviceType);
            result = executor.execute(command);
            breaker.recordSuccess();

            logCommand(command, "SUCCESS", result);

        } catch (IllegalArgumentException e) {
            breaker.recordFailure();
            logCommand(command, "INVALID", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "Invalid command: " + e.getMessage()));

        } catch (IllegalStateException e) {
            breaker.recordFailure();
            logCommand(command, "UNAVAILABLE", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error(503, "Device unavailable: " + e.getMessage()));

        } catch (ArithmeticException e) {
            breaker.recordFailure();
            logCommand(command, "FAILED", "Calculation error: " + e.getMessage());
            log.error("Arithmetic error in IOT command execution", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Calculation error"));

        } catch (Exception e) {
            breaker.recordFailure();
            logCommand(command, "FAILED", e.getMessage());
            log.error("Unexpected error in IOT command", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Internal error"));
        }

        return ResponseEntity.ok(ApiResponse.success(result));
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

    private void logCommand(DeviceCommand command, String status, String result) {
        try {
            DeviceCommandLog logEntry = DeviceCommandLog.builder()
                    .commandId(command.getCommandId())
                    .tankId(command.getTankId())
                    .deviceType(command.getDeviceType().getCode())
                    .action(command.getAction())
                    .params(command.getParams().toString())
                    .status(status)
                    .result(result)
                    .issuedAt(command.getIssuedAt())
                    .executedAt(LocalDateTime.now())
                    .build();
            commandLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.error("Failed to log command result for commandId={}", command.getCommandId(), e);
        }
    }
}
