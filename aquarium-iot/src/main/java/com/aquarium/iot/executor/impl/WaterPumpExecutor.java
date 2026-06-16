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
public class WaterPumpExecutor implements DeviceCommandExecutor {

    private final DeviceProtocolSender protocolSender;

    @Override
    public String execute(DeviceCommand command) {
        String action = command.getAction();
        Map<String, Object> params = command.getParams();

        log.info("Executing water pump command: tankId={}, action={}", command.getTankId(), action);

        String protocolCommand;
        switch (action) {
            case "on" -> {
                int duration = params.containsKey("duration_seconds")
                        ? ((Number) params.get("duration_seconds")).intValue()
                        : 60;
                String subAction = (String) params.getOrDefault("action", "circulate");
                protocolCommand = String.format("PUMP:WATER:ON:ACTION=%s:DURATION=%d", subAction, duration);
                protocolSender.sendToDevice(command.getTankId(), DeviceType.WATER_PUMP, protocolCommand);
                return String.format("Water pump ON: %s for %d seconds", subAction, duration);
            }
            case "off" -> {
                protocolCommand = "PUMP:WATER:OFF";
                protocolSender.sendToDevice(command.getTankId(), DeviceType.WATER_PUMP, protocolCommand);
                return "Water pump turned OFF";
            }
            default -> throw new IllegalArgumentException("Unknown pump action: " + action);
        }
    }

    @Override
    public String getDeviceType() {
        return DeviceType.WATER_PUMP.getCode();
    }
}
