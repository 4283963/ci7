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
public class HeaterExecutor implements DeviceCommandExecutor {

    private final DeviceProtocolSender protocolSender;

    @Override
    public String execute(DeviceCommand command) {
        String action = command.getAction();
        Map<String, Object> params = command.getParams();

        log.info("Executing heater command: tankId={}, action={}, params={}",
                command.getTankId(), action, params);

        String protocolCommand;
        switch (action) {
            case "on" -> {
                double targetTemp = params.containsKey("target_temp")
                        ? ((Number) params.get("target_temp")).doubleValue()
                        : 25.0;
                protocolCommand = String.format("HEATER:ON:TARGET=%.1f", targetTemp);
                protocolSender.sendToDevice(command.getTankId(), DeviceType.HEATER, protocolCommand);
                return String.format("Heater turned ON, target temperature: %.1f℃", targetTemp);
            }
            case "off" -> {
                protocolCommand = "HEATER:OFF";
                protocolSender.sendToDevice(command.getTankId(), DeviceType.HEATER, protocolCommand);
                return "Heater turned OFF";
            }
            case "set_temp" -> {
                double targetTemp = ((Number) params.get("target_temp")).doubleValue();
                protocolCommand = String.format("HEATER:SET:TARGET=%.1f", targetTemp);
                protocolSender.sendToDevice(command.getTankId(), DeviceType.HEATER, protocolCommand);
                return String.format("Heater target temperature set to %.1f℃", targetTemp);
            }
            default -> throw new IllegalArgumentException("Unknown heater action: " + action);
        }
    }

    @Override
    public String getDeviceType() {
        return DeviceType.HEATER.getCode();
    }
}
