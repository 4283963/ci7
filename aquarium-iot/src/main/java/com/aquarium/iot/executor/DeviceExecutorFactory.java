package com.aquarium.iot.executor;

import com.aquarium.common.enums.DeviceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class DeviceExecutorFactory {

    private final Map<String, DeviceCommandExecutor> executorMap = new ConcurrentHashMap<>();

    public DeviceExecutorFactory(List<DeviceCommandExecutor> executors) {
        for (DeviceCommandExecutor executor : executors) {
            executorMap.put(executor.getDeviceType(), executor);
        }
        log.info("Registered {} device executors: {}", executorMap.size(), executorMap.keySet());
    }

    public DeviceCommandExecutor getExecutor(DeviceType deviceType) {
        DeviceCommandExecutor executor = executorMap.get(deviceType.getCode());
        if (executor == null) {
            throw new IllegalArgumentException("No executor found for device type: " + deviceType);
        }
        return executor;
    }
}
