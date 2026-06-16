package com.aquarium.service.water;

import com.aquarium.common.dto.DeviceCommand;
import com.aquarium.common.dto.SensorData;
import com.aquarium.common.dto.WaterQualityAlert;
import com.aquarium.common.enums.AlertLevel;
import com.aquarium.common.enums.DeviceType;
import com.aquarium.service.entity.WaterQualityRecord;
import com.aquarium.service.entity.AlertRecord;
import com.aquarium.service.mapper.WaterQualityRecordMapper;
import com.aquarium.service.mapper.AlertRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaterQualityService {

    private final WaterQualityThresholds thresholds;
    private final WaterQualityRecordMapper recordMapper;
    private final AlertRecordMapper alertRecordMapper;
    private final RabbitTemplate rabbitTemplate;
    private final SensorDataValidator validator;

    private static final String DEVICE_COMMAND_EXCHANGE = "aquarium.device.command";
    private static final String ALERT_EXCHANGE = "aquarium.alert";

    private final ConcurrentHashMap<String, SensorData> latestData = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> consecutiveAnomalies = new ConcurrentHashMap<>();
    private static final int AUTO_CONTROL_THRESHOLD = 3;

    public void processSensorData(SensorData data) {
        SensorDataValidator.ValidationResult validation = validator.validate(data);
        if (!validation.isValid()) {
            log.warn("Sensor data rejected: deviceId={}, reason={}",
                    data != null ? data.getDeviceId() : "null", validation.getMessage());
            return;
        }

        String tankId = resolveTankId(data.getDeviceId());
        data.setTimestamp(LocalDateTime.now());
        latestData.put(tankId, data);

        WaterQualityRecord record = buildRecord(tankId, data);
        recordMapper.insert(record);

        if (validation.isSuspicious()) {
            log.info("Sensor data flagged as suspicious but accepted: tankId={}, reason={}",
                    tankId, validation.getMessage());
        }

        List<WaterQualityAlert> alerts = thresholds.evaluate(data, tankId);

        if (!alerts.isEmpty()) {
            handleAlerts(tankId, alerts);
        } else {
            consecutiveAnomalies.remove(tankId);
        }

        log.info("Processed sensor data: tankId={}, temp={}, ph={}, cl={}, do={}",
                tankId, data.getTemperature(), data.getPh(),
                data.getChlorine(), data.getDissolvedOxygen());
    }

    public SensorData getLatestData(String tankId) {
        return latestData.get(tankId);
    }

    public Map<String, SensorData> getAllLatestData() {
        return new HashMap<>(latestData);
    }

    private void handleAlerts(String tankId, List<WaterQualityAlert> alerts) {
        int anomalyCount = consecutiveAnomalies.merge(tankId, 1, Integer::sum);

        for (WaterQualityAlert alert : alerts) {
            AlertRecord alertRecord = AlertRecord.builder()
                    .alertId(alert.getAlertId())
                    .tankId(tankId)
                    .metric(alert.getMetric().getCode())
                    .actualValue(alert.getActualValue())
                    .thresholdMin(alert.getThresholdMin())
                    .thresholdMax(alert.getThresholdMax())
                    .level(alert.getLevel().getCode())
                    .message(alert.getMessage())
                    .createdAt(LocalDateTime.now())
                    .build();
            alertRecordMapper.insert(alertRecord);

            rabbitTemplate.convertAndSend(ALERT_EXCHANGE, "alert." + tankId, alert);
            log.warn("Water quality alert: tankId={}, level={}, metric={}, msg={}",
                    tankId, alert.getLevel(), alert.getMetric(), alert.getMessage());
        }

        if (anomalyCount >= AUTO_CONTROL_THRESHOLD) {
            triggerAutoControl(tankId, alerts);
        }
    }

    private void triggerAutoControl(String tankId, List<WaterQualityAlert> alerts) {
        for (WaterQualityAlert alert : alerts) {
            if (alert.getLevel() != AlertLevel.CRITICAL) {
                continue;
            }

            DeviceCommand command = null;

            switch (alert.getMetric()) {
                case TEMPERATURE -> {
                    double temp = alert.getActualValue();
                    if (temp < alert.getThresholdMin()) {
                        command = buildDeviceCommand(tankId, DeviceType.HEATER, "on",
                                Map.of("target_temp", alert.getThresholdMin()));
                    } else if (temp > alert.getThresholdMax()) {
                        command = buildDeviceCommand(tankId, DeviceType.HEATER, "off", Map.of());
                    }
                }
                case CHLORINE -> {
                    if (alert.getActualValue() > alert.getThresholdMax()) {
                        int drainSeconds = WaterChemistryCalculator.estimateChlorineDrainSeconds(
                                alert.getActualValue(),
                                alert.getThresholdMax(),
                                500.0,
                                5.0);
                        command = buildDeviceCommand(tankId, DeviceType.CHLORINE_VALVE, "open",
                                Map.of("duration_seconds", drainSeconds));
                    }
                }
                case DISSOLVED_OXYGEN -> {
                    if (alert.getActualValue() < alert.getThresholdMin()) {
                        command = buildDeviceCommand(tankId, DeviceType.AERATION_PUMP, "on",
                                Map.of("duration_seconds", 120));
                    }
                }
                case PH -> {
                    if (alert.getActualValue() < alert.getThresholdMin()) {
                        command = buildDeviceCommand(tankId, DeviceType.WATER_PUMP, "on",
                                Map.of("action", "add_buffer", "duration_seconds", 30));
                    }
                }
            }

            if (command != null) {
                rabbitTemplate.convertAndSend(DEVICE_COMMAND_EXCHANGE,
                        "device.command." + command.getDeviceType().getCode(), command);
                log.info("Auto control triggered: tankId={}, device={}, action={}",
                        tankId, command.getDeviceType(), command.getAction());
            }
        }
    }

    private DeviceCommand buildDeviceCommand(String tankId, DeviceType deviceType,
                                              String action, Map<String, Object> params) {
        return DeviceCommand.builder()
                .commandId(UUID.randomUUID().toString())
                .tankId(tankId)
                .deviceType(deviceType)
                .action(action)
                .params(params)
                .issuedAt(LocalDateTime.now())
                .build();
    }

    private String resolveTankId(String deviceId) {
        return "TANK-" + deviceId;
    }

    private WaterQualityRecord buildRecord(String tankId, SensorData data) {
        return WaterQualityRecord.builder()
                .tankId(tankId)
                .deviceId(data.getDeviceId())
                .temperature(data.getTemperature())
                .ph(data.getPh())
                .chlorine(data.getChlorine())
                .dissolvedOxygen(data.getDissolvedOxygen())
                .recordedAt(LocalDateTime.now())
                .build();
    }
}
