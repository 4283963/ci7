package com.aquarium.iot.controller;

import com.aquarium.common.dto.DeviceCommand;
import com.aquarium.iot.circuit.CircuitBreakerRegistry;
import com.aquarium.iot.executor.DeviceExecutorFactory;
import com.aquarium.iot.executor.DeviceCommandExecutor;
import com.aquarium.iot.entity.DeviceCommandLog;
import com.aquarium.iot.mapper.DeviceCommandLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceCommandConsumer {

    private final DeviceExecutorFactory executorFactory;
    private final DeviceCommandLogMapper commandLogMapper;
    private final ObjectMapper objectMapper;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @RabbitListener(queues = "aquarium.queue.device.command")
    public void onDeviceCommand(DeviceCommand command) {
        String deviceType = command.getDeviceType().getCode();
        var breaker = circuitBreakerRegistry.get("device:" + deviceType);

        log.info("Received device command: commandId={}, tankId={}, type={}, action={}, breaker={}",
                command.getCommandId(), command.getTankId(),
                command.getDeviceType(), command.getAction(), breaker.getState());

        if (!breaker.allowRequest()) {
            log.warn("Circuit breaker OPEN for device type {}, rejecting command {}",
                    deviceType, command.getCommandId());
            logRejection(command, "CIRCUIT_OPEN");
            return;
        }

        DeviceCommandLog logEntry = DeviceCommandLog.builder()
                .commandId(command.getCommandId())
                .tankId(command.getTankId())
                .deviceType(deviceType)
                .action(command.getAction())
                .issuedAt(command.getIssuedAt())
                .status("RECEIVED")
                .build();

        try {
            logEntry.setParams(objectMapper.writeValueAsString(command.getParams()));

            DeviceCommandExecutor executor = executorFactory.getExecutor(command.getDeviceType());
            String result = executor.execute(command);

            logEntry.setStatus("SUCCESS");
            logEntry.setResult(result);
            logEntry.setExecutedAt(LocalDateTime.now());

            breaker.recordSuccess();

            log.info("Device command executed: commandId={}, type={}, result={}",
                    command.getCommandId(), deviceType, result);

        } catch (IllegalArgumentException e) {
            breaker.recordFailure();
            logEntry.setStatus("INVALID");
            logEntry.setResult("Invalid argument: " + e.getMessage());
            logEntry.setExecutedAt(LocalDateTime.now());
            log.warn("Invalid device command: commandId={}, error={}", command.getCommandId(), e.getMessage());

        } catch (IllegalStateException e) {
            breaker.recordFailure();
            logEntry.setStatus("UNAVAILABLE");
            logEntry.setResult("Device unavailable: " + e.getMessage());
            logEntry.setExecutedAt(LocalDateTime.now());
            log.warn("Device unavailable: commandId={}, error={}", command.getCommandId(), e.getMessage());

        } catch (ArithmeticException e) {
            breaker.recordFailure();
            logEntry.setStatus("FAILED");
            logEntry.setResult("Calculation error: " + e.getMessage());
            logEntry.setExecutedAt(LocalDateTime.now());
            log.error("Arithmetic error in device command: commandId={}", command.getCommandId(), e);

        } catch (Exception e) {
            breaker.recordFailure();
            logEntry.setStatus("FAILED");
            logEntry.setResult("Internal error: " + e.getMessage());
            logEntry.setExecutedAt(LocalDateTime.now());
            log.error("Unexpected error executing device command: commandId={}", command.getCommandId(), e);
        }

        try {
            commandLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.error("Failed to write command log for commandId={}", command.getCommandId(), e);
        }
    }

    private void logRejection(DeviceCommand command, String reason) {
        try {
            DeviceCommandLog logEntry = DeviceCommandLog.builder()
                    .commandId(command.getCommandId())
                    .tankId(command.getTankId())
                    .deviceType(command.getDeviceType().getCode())
                    .action(command.getAction())
                    .issuedAt(command.getIssuedAt())
                    .status("REJECTED")
                    .result(reason)
                    .executedAt(LocalDateTime.now())
                    .build();
            commandLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.error("Failed to log rejection for commandId={}", command.getCommandId(), e);
        }
    }
}
