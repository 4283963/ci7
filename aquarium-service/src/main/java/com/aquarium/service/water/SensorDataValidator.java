package com.aquarium.service.water;

import com.aquarium.common.dto.SensorData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SensorDataValidator {

    private static final double MAX_TEMP = 45.0;
    private static final double MIN_TEMP = 0.0;
    private static final double MAX_PH = 14.0;
    private static final double MIN_PH = 0.0;
    private static final double MAX_CHLORINE = 10.0;
    private static final double MAX_DO = 50.0;

    private static final double MAX_TEMP_SPIKE = 5.0;
    private static final double MAX_PH_SPIKE = 2.0;
    private static final double MAX_CHLORINE_SPIKE = 0.5;
    private static final double MAX_DO_SPIKE = 5.0;

    private final ConcurrentHashMap<String, SensorData> lastValidData = new ConcurrentHashMap<>();

    public ValidationResult validate(SensorData data) {
        if (data == null) {
            return ValidationResult.invalid("Sensor data is null");
        }

        if (data.getDeviceId() == null || data.getDeviceId().isEmpty()) {
            return ValidationResult.invalid("Device ID is empty");
        }

        if (!isValidRange(data.getTemperature(), MIN_TEMP, MAX_TEMP)) {
            log.warn("Temperature out of range: deviceId={}, value={}",
                    data.getDeviceId(), data.getTemperature());
            return ValidationResult.invalid("Temperature out of range: " + data.getTemperature());
        }

        if (!isValidRange(data.getPh(), MIN_PH, MAX_PH)) {
            log.warn("pH out of range: deviceId={}, value={}",
                    data.getDeviceId(), data.getPh());
            return ValidationResult.invalid("pH out of range: " + data.getPh());
        }

        if (!isValidRange(data.getChlorine(), 0.0, MAX_CHLORINE)) {
            log.warn("Chlorine out of range: deviceId={}, value={}",
                    data.getDeviceId(), data.getChlorine());
            return ValidationResult.invalid("Chlorine out of range: " + data.getChlorine());
        }

        if (!isValidRange(data.getDissolvedOxygen(), 0.0, MAX_DO)) {
            log.warn("Dissolved oxygen out of range: deviceId={}, value={}",
                    data.getDeviceId(), data.getDissolvedOxygen());
            return ValidationResult.invalid("DO out of range: " + data.getDissolvedOxygen());
        }

        SensorData previous = lastValidData.get(data.getDeviceId());
        if (previous != null) {
            if (isSpike(data.getTemperature(), previous.getTemperature(), MAX_TEMP_SPIKE)) {
                log.warn("Temperature spike detected: deviceId={}, prev={}, curr={}",
                        data.getDeviceId(), previous.getTemperature(), data.getTemperature());
                return ValidationResult.suspicious("Temperature spike detected");
            }
            if (isSpike(data.getPh(), previous.getPh(), MAX_PH_SPIKE)) {
                log.warn("pH spike detected: deviceId={}, prev={}, curr={}",
                        data.getDeviceId(), previous.getPh(), data.getPh());
                return ValidationResult.suspicious("pH spike detected");
            }
            if (isSpike(data.getChlorine(), previous.getChlorine(), MAX_CHLORINE_SPIKE)) {
                log.warn("Chlorine spike detected: deviceId={}, prev={}, curr={}",
                        data.getDeviceId(), previous.getChlorine(), data.getChlorine());
                return ValidationResult.suspicious("Chlorine spike detected");
            }
            if (isSpike(data.getDissolvedOxygen(), previous.getDissolvedOxygen(), MAX_DO_SPIKE)) {
                log.warn("DO spike detected: deviceId={}, prev={}, curr={}",
                        data.getDeviceId(), previous.getDissolvedOxygen(), data.getDissolvedOxygen());
                return ValidationResult.suspicious("DO spike detected");
            }
        }

        lastValidData.put(data.getDeviceId(), data);
        return ValidationResult.valid();
    }

    private boolean isValidRange(Double value, double min, double max) {
        if (value == null) return false;
        if (value.isNaN() || value.isInfinite()) return false;
        return value >= min && value <= max;
    }

    private boolean isSpike(double current, double previous, double maxDelta) {
        return Math.abs(current - previous) > maxDelta;
    }

    public static class ValidationResult {
        private final boolean valid;
        private final boolean suspicious;
        private final String message;

        private ValidationResult(boolean valid, boolean suspicious, String message) {
            this.valid = valid;
            this.suspicious = suspicious;
            this.message = message;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, false, null);
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, false, message);
        }

        public static ValidationResult suspicious(String message) {
            return new ValidationResult(true, true, message);
        }

        public boolean isValid() { return valid; }
        public boolean isSuspicious() { return suspicious; }
        public String getMessage() { return message; }
    }
}
