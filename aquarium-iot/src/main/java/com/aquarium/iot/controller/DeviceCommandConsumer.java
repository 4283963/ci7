package com.aquarium.iot.controller;

import com.aquarium.common.dto.DeviceCommand;
import com.aquarium.common.enums.DeviceType;
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
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceCommandConsumer {

    private final DeviceExecutorFactory executorFactory;
    private final DeviceCommandLogMapper commandLogMapper;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "aquarium.queue.device.command")
    public void onDeviceCommand(DeviceCommand command) {
        log.info("Received device command: commandId={}, tankId={}, type={}, action={}",
                command.getCommandId(), command.getTankId(),
                command.getDeviceType(), command.getAction());

        DeviceCommandLog logEntry = DeviceCommandLog.builder()
                .commandId(command.getCommandId())
                .tankId(command.getTankId())
                .deviceType(command.getDeviceType().getCode())
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

            log.info("Device command executed successfully: commandId={}, result={}",
                    command.getCommandId(), result);

        } catch (Exception e) {
            logEntry.setStatus("FAILED");
            logEntry.setResult(e.getMessage());
            logEntry.setExecutedAt(LocalDateTime.now());

            log.error("Device command execution failed: commandId={}", command.getCommandId(), e);
        }

        commandLogMapper.insert(logEntry);
    }
}
