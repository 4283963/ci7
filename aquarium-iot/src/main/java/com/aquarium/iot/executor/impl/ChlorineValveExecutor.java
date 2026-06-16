package com.aquarium.iot.executor.impl;

import com.aquarium.common.dto.DeviceCommand;
import com.aquarium.common.enums.DeviceType;
import com.aquarium.iot.executor.DeviceCommandExecutor;
import com.aquarium.iot.protocol.DeviceProtocolSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChlorineValveExecutor implements DeviceCommandExecutor {

    private final DeviceProtocolSender protocolSender;

    @Override
    public String execute(DeviceCommand command) {
        String action = command.getAction();
        Map<String, Object> params = command.getParams();

        log.info("Executing chlorine valve command: tankId={}, action={}, params={}",
                command.getTankId(), action, params);

        String protocolCommand;
        switch (action) {
            case "open" -> {
                int duration = params.containsKey("duration_seconds")
                        ? ((Number) params.get("duration_seconds")).intValue()
                        : 60;
                protocolCommand = String.format("VALVE:CHLORINE:OPEN:DURATION=%d", duration);
                protocolSender.sendToDevice(command.getTankId(), DeviceType.CHLORINE_VALVE, protocolCommand);

                scheduleAutoClose(command.getTankId(), duration);
                return String.format("Chlorine drain valve opened for %d seconds", duration);
            }
            case "close" -> {
                protocolCommand = "VALVE:CHLORINE:CLOSE";
                protocolSender.sendToDevice(command.getTankId(), DeviceType.CHLORINE_VALVE, protocolCommand);
                return "Chlorine drain valve closed";
            }
            case "refill" -> {
                int duration = params.containsKey("duration_seconds")
                        ? ((Number) params.get("duration_seconds")).intValue()
                        : 30;
                protocolCommand = String.format("VALVE:REFILL:OPEN:DURATION=%d", duration);
                protocolSender.sendToDevice(command.getTankId(), DeviceType.CHLORINE_VALVE, protocolCommand);
                return String.format("Refill valve opened for %d seconds", duration);
            }
            default -> throw new IllegalArgumentException("Unknown valve action: " + action);
        }
    }

    private void scheduleAutoClose(String tankId, int delaySeconds) {
        Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep(delaySeconds * 1000L);
                String closeCommand = "VALVE:CHLORINE:CLOSE";
                protocolSender.sendToDevice(tankId, DeviceType.CHLORINE_VALVE, closeCommand);
                log.info("Auto-closed chlorine valve for tank {} after {} seconds", tankId, delaySeconds);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    @Override
    public String getDeviceType() {
        return DeviceType.CHLORINE_VALVE.getCode();
    }
}
