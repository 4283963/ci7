package com.aquarium.service.breeding;

import com.aquarium.common.dto.DeviceCommand;
import com.aquarium.common.enums.DeviceType;
import com.aquarium.service.entity.BreedingConfig;
import com.aquarium.service.mapper.BreedingConfigMapper;
import com.aquarium.service.websocket.AquariumWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BreedingService {

    private final BreedingConfigMapper configMapper;
    private final BreedingSpeciesPresets presets;
    private final RabbitTemplate rabbitTemplate;
    private final AquariumWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    private static final String DEVICE_COMMAND_EXCHANGE = "aquarium.device.command";

    public List<BreedingConfig> getAllConfigs() {
        return configMapper.selectList(null);
    }

    public BreedingConfig getConfigByTank(String tankId) {
        return configMapper.findByTankId(tankId);
    }

    public List<BreedingConfig> getActiveBreedingConfigs() {
        return configMapper.findActiveBreeding();
    }

    public BreedingConfig updateConfig(String tankId, BreedingConfig config) {
        BreedingConfig existing = configMapper.findByTankId(tankId);
        if (existing == null) {
            config.setTankId(tankId);
            configMapper.insert(config);
            return config;
        }
        config.setId(existing.getId());
        config.setTankId(tankId);
        config.setUpdatedAt(LocalDateTime.now());
        configMapper.updateById(config);
        return configMapper.findByTankId(tankId);
    }

    public BreedingConfig startBreeding(String tankId, String species) {
        BreedingConfig config = configMapper.findByTankId(tankId);
        if (config == null) {
            config = new BreedingConfig();
            config.setTankId(tankId);
        }

        BreedingSpeciesPresets.SpeciesPreset preset = presets.getPreset(species);

        config.setSpecies(species);
        config.setModeStatus("preparing");
        config.setTargetTemp(preset.getTargetTemp());
        config.setTargetPh(preset.getTargetPh());
        config.setWaterFlowLevel(preset.getWaterFlowLevel());
        config.setFilterPumpEnabled(preset.isFilterPumpEnabled());
        config.setAerationLevel(preset.getAerationLevel());
        config.setStartTime(LocalDateTime.now());

        LocalDateTime expectedBirth = LocalDateTime.now().plusDays(preset.getPregnancyDays());
        config.setExpectedBirthTime(expectedBirth);
        config.setMotherRemovalTime(expectedBirth.plusHours(preset.getMotherRemovalHoursAfterBirth()));
        config.setFrySafeTime(expectedBirth.plusDays(preset.getFrySafeDays()));

        config.setUpdatedAt(LocalDateTime.now());

        if (config.getId() == null) {
            configMapper.insert(config);
        } else {
            configMapper.updateById(config);
        }

        applyBreedingSettings(tankId, preset);
        broadcastUpdate(tankId, "BREEDING_STARTED", config);

        log.info("Breeding mode started: tankId={}, species={}, expectedBirth={}",
                tankId, species, expectedBirth);

        return configMapper.findByTankId(tankId);
    }

    public BreedingConfig stopBreeding(String tankId) {
        BreedingConfig config = configMapper.findByTankId(tankId);
        if (config == null) {
            return null;
        }

        config.setModeStatus("idle");
        config.setStartTime(null);
        config.setExpectedBirthTime(null);
        config.setMotherRemovalTime(null);
        config.setFrySafeTime(null);
        config.setUpdatedAt(LocalDateTime.now());
        configMapper.updateById(config);

        restoreNormalSettings(tankId);
        broadcastUpdate(tankId, "BREEDING_STOPPED", config);

        log.info("Breeding mode stopped: tankId={}", tankId);
        return config;
    }

    public BreedingConfig markAsBirthed(String tankId) {
        BreedingConfig config = configMapper.findByTankId(tankId);
        if (config == null) {
            return null;
        }

        BreedingSpeciesPresets.SpeciesPreset preset = presets.getPreset(config.getSpecies());
        LocalDateTime now = LocalDateTime.now();

        config.setModeStatus("hatching");
        config.setMotherRemovalTime(now.plusHours(preset.getMotherRemovalHoursAfterBirth()));
        config.setFrySafeTime(now.plusDays(preset.getFrySafeDays()));
        config.setUpdatedAt(LocalDateTime.now());
        configMapper.updateById(config);

        broadcastUpdate(tankId, "BIRTH_DETECTED", config);
        log.info("Birth detected: tankId={}, motherRemovalAt={}", tankId, config.getMotherRemovalTime());

        return config;
    }

    public Map<String, Object> getCountdown(String tankId) {
        BreedingConfig config = configMapper.findByTankId(tankId);
        if (config == null || "idle".equals(config.getModeStatus())) {
            return Map.of("active", false, "modeStatus", "idle");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("active", true);
        result.put("modeStatus", config.getModeStatus());
        result.put("tankId", tankId);
        result.put("species", config.getSpecies());
        result.put("speciesName", presets.getPreset(config.getSpecies()).getName());

        LocalDateTime now = LocalDateTime.now();

        if (config.getExpectedBirthTime() != null) {
            result.put("expectedBirthTime", config.getExpectedBirthTime());
            result.put("birthCountdown", formatDuration(now, config.getExpectedBirthTime()));
            result.put("birthCountdownSeconds", ChronoUnit.SECONDS.between(now, config.getExpectedBirthTime()));
        }

        if (config.getMotherRemovalTime() != null) {
            result.put("motherRemovalTime", config.getMotherRemovalTime());
            result.put("motherRemovalCountdown", formatDuration(now, config.getMotherRemovalTime()));
            result.put("motherRemovalCountdownSeconds", ChronoUnit.SECONDS.between(now, config.getMotherRemovalTime()));
            result.put("motherRemovalDue", now.isAfter(config.getMotherRemovalTime()));
        }

        if (config.getFrySafeTime() != null) {
            result.put("frySafeTime", config.getFrySafeTime());
            result.put("frySafeCountdown", formatDuration(now, config.getFrySafeTime()));
            result.put("frySafeCountdownSeconds", ChronoUnit.SECONDS.between(now, config.getFrySafeTime()));
        }

        result.put("targetTemp", config.getTargetTemp());
        result.put("targetPh", config.getTargetPh());
        result.put("waterFlowLevel", config.getWaterFlowLevel());
        result.put("filterPumpEnabled", config.getFilterPumpEnabled());
        result.put("aerationLevel", config.getAerationLevel());
        result.put("startTime", config.getStartTime());
        result.put("note", config.getNote());

        return result;
    }

    public Map<String, Map<String, Object>> getAllCountdowns() {
        List<BreedingConfig> active = getActiveBreedingConfigs();
        Map<String, Map<String, Object>> result = new HashMap<>();
        for (BreedingConfig config : active) {
            result.put(config.getTankId(), getCountdown(config.getTankId()));
        }
        return result;
    }

    private void applyBreedingSettings(String tankId, BreedingSpeciesPresets.SpeciesPreset preset) {
        if (preset.isFilterPumpEnabled()) {
            sendDeviceCommand(tankId, DeviceType.WATER_PUMP, "off", Map.of());
            log.info("Auto-stopped filter pump for breeding: tankId={}", tankId);
        }

        sendDeviceCommand(tankId, DeviceType.HEATER, "set_temp",
                Map.of("target_temp", preset.getTargetTemp()));

        String aerationAction = switch (preset.getAerationLevel()) {
            case "low" -> "on";
            case "medium" -> "on";
            case "high" -> "on";
            default -> "off";
        };
        int duration = preset.getAerationLevel().equals("high") ? 180 : 120;
        sendDeviceCommand(tankId, DeviceType.AERATION_PUMP, aerationAction,
                Map.of("duration_seconds", duration, "level", preset.getAerationLevel()));

        log.info("Breeding settings applied: tankId={}, temp={}℃, filter=off, aeration={}",
                tankId, preset.getTargetTemp(), preset.getAerationLevel());
    }

    private void restoreNormalSettings(String tankId) {
        sendDeviceCommand(tankId, DeviceType.WATER_PUMP, "on",
                Map.of("action", "filter", "duration_seconds", -1));
        sendDeviceCommand(tankId, DeviceType.HEATER, "set_temp",
                Map.of("target_temp", 25.0));
        sendDeviceCommand(tankId, DeviceType.AERATION_PUMP, "off", Map.of());

        log.info("Normal settings restored: tankId={}", tankId);
    }

    private void sendDeviceCommand(String tankId, DeviceType type, String action, Map<String, Object> params) {
        try {
            DeviceCommand command = DeviceCommand.builder()
                    .commandId(UUID.randomUUID().toString())
                    .tankId(tankId)
                    .deviceType(type)
                    .action(action)
                    .params(params)
                    .issuedAt(LocalDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(DEVICE_COMMAND_EXCHANGE,
                    "device.command." + type.getCode(), command);
        } catch (Exception e) {
            log.warn("Failed to send breeding device command: tankId={}, type={}", tankId, type, e);
        }
    }

    private String formatDuration(LocalDateTime from, LocalDateTime to) {
        long seconds = ChronoUnit.SECONDS.between(from, to);
        if (seconds < 0) {
            return "已到期";
        }
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (days > 0) {
            return String.format("%d天%d小时", days, hours);
        } else if (hours > 0) {
            return String.format("%d小时%d分", hours, minutes);
        } else {
            return String.format("%d分%d秒", minutes, secs);
        }
    }

    private void broadcastUpdate(String tankId, String eventType, Object data) {
        try {
            Map<String, Object> payload = Map.of(
                    "event", eventType,
                    "tankId", tankId,
                    "data", data
            );
            webSocketHandler.broadcastMessage("BREEDING_UPDATE", payload);
        } catch (Exception e) {
            log.warn("Failed to broadcast breeding update", e);
        }
    }

    @Scheduled(fixedRate = 30000)
    public void checkBreedingReminders() {
        List<BreedingConfig> active = getActiveBreedingConfigs();
        LocalDateTime now = LocalDateTime.now();

        for (BreedingConfig config : active) {
            if (!Boolean.TRUE.equals(config.getReminderEnabled())) {
                continue;
            }

            if (config.getMotherRemovalTime() != null &&
                    now.isAfter(config.getMotherRemovalTime()) &&
                    now.isBefore(config.getMotherRemovalTime().plusHours(1))) {
                broadcastReminder(config.getTankId(), "MOTHER_REMOVAL_DUE",
                        "🐟 母鱼捞取时间到！" + config.getTankId() + " 的母鱼应捞出，避免吃苗");
                log.info("Breeding reminder: mother removal due for {}", config.getTankId());
            }

            if (config.getExpectedBirthTime() != null &&
                    now.isAfter(config.getExpectedBirthTime().minusHours(24)) &&
                    now.isBefore(config.getExpectedBirthTime().minusHours(23))) {
                broadcastReminder(config.getTankId(), "BIRTH_SOON",
                        "⏰ " + config.getTankId() + " 预计24小时内产仔，请做好准备");
                log.info("Breeding reminder: birth soon for {}", config.getTankId());
            }
        }
    }

    private void broadcastReminder(String tankId, String eventType, String message) {
        try {
            Map<String, Object> payload = Map.of(
                    "event", eventType,
                    "tankId", tankId,
                    "message", message
            );
            webSocketHandler.broadcastMessage("BREEDING_REMINDER", payload);
        } catch (Exception e) {
            log.warn("Failed to broadcast breeding reminder", e);
        }
    }
}
