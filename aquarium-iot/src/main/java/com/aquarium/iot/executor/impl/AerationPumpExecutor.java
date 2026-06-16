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
public class AerationPumpExecutor implements DeviceCommandExecutor {

    private final DeviceProtocolSender protocolSender;

    @Override
    public String execute(DeviceCommand command) {
        String action = command.getAction();
        Map<String, Object> params = command.getParams();

        log.info("Executing aeration pump command: tankId={}, action={}", command.getTankId(), action);

        String protocolCommand;
        switch (action) {
            case "on" -> {
                int duration = params.containsKey("duration_seconds")
                        ? ((Number) params.get("duration_seconds")).intValue()
                        : 120;
                protocolCommand = String.format("PUMP:AERATION:ON:DURATION=%d", duration);
                protocolSender.sendToDevice(command.getTankId(), DeviceType.AERATION_PUMP, protocolCommand);
                return String.format("Aeration pump ON for %d seconds", duration);
            }
            case "off" -> {
                protocolCommand = "PUMP:AERATION:OFF";
                protocolSender.sendToDevice(command.getTankId(), DeviceType.AERATION_PUMP, protocolCommand);
                return "Aeration pump turned OFF";
            }
            default -> throw new IllegalArgumentException("Unknown aeration action: " + action);
        }
    }

    @Override
    public String getDeviceType() {
        return DeviceType.AERATION_PUMP.getCode();
    }
}
