package com.aquarium.iot.executor;

import com.aquarium.common.dto.DeviceCommand;

public interface DeviceCommandExecutor {
    String execute(DeviceCommand command);
    String getDeviceType();
}
