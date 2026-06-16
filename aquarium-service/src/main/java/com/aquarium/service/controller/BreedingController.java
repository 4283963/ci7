package com.aquarium.service.controller;

import com.aquarium.common.dto.ApiResponse;
import com.aquarium.service.breeding.BreedingService;
import com.aquarium.service.breeding.BreedingSpeciesPresets;
import com.aquarium.service.entity.BreedingConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/breeding")
@RequiredArgsConstructor
public class BreedingController {

    private final BreedingService breedingService;
    private final BreedingSpeciesPresets presets;

    @GetMapping("/configs")
    public ApiResponse<List<BreedingConfig>> getAllConfigs() {
        return ApiResponse.success(breedingService.getAllConfigs());
    }

    @GetMapping("/config/{tankId}")
    public ApiResponse<BreedingConfig> getConfig(@PathVariable String tankId) {
        BreedingConfig config = breedingService.getConfigByTank(tankId);
        if (config == null) {
            return ApiResponse.error(404, "Breeding config not found for tank: " + tankId);
        }
        return ApiResponse.success(config);
    }

    @PutMapping("/config/{tankId}")
    public ApiResponse<BreedingConfig> updateConfig(
            @PathVariable String tankId,
            @RequestBody BreedingConfig config) {
        BreedingConfig updated = breedingService.updateConfig(tankId, config);
        return ApiResponse.success(updated);
    }

    @PostMapping("/start/{tankId}")
    public ApiResponse<BreedingConfig> startBreeding(
            @PathVariable String tankId,
            @RequestParam(defaultValue = "guppy") String species) {
        BreedingConfig config = breedingService.startBreeding(tankId, species);
        return ApiResponse.success(config);
    }

    @PostMapping("/stop/{tankId}")
    public ApiResponse<BreedingConfig> stopBreeding(@PathVariable String tankId) {
        BreedingConfig config = breedingService.stopBreeding(tankId);
        if (config == null) {
            return ApiResponse.error(404, "Tank not found: " + tankId);
        }
        return ApiResponse.success(config);
    }

    @PostMapping("/mark-birthed/{tankId}")
    public ApiResponse<BreedingConfig> markAsBirthed(@PathVariable String tankId) {
        BreedingConfig config = breedingService.markAsBirthed(tankId);
        if (config == null) {
            return ApiResponse.error(404, "Tank not found: " + tankId);
        }
        return ApiResponse.success(config);
    }

    @GetMapping("/countdown/{tankId}")
    public ApiResponse<Map<String, Object>> getCountdown(@PathVariable String tankId) {
        Map<String, Object> countdown = breedingService.getCountdown(tankId);
        return ApiResponse.success(countdown);
    }

    @GetMapping("/countdown")
    public ApiResponse<Map<String, Map<String, Object>>> getAllCountdowns() {
        return ApiResponse.success(breedingService.getAllCountdowns());
    }

    @GetMapping("/active")
    public ApiResponse<List<BreedingConfig>> getActiveBreeding() {
        return ApiResponse.success(breedingService.getActiveBreedingConfigs());
    }

    @GetMapping("/species")
    public ApiResponse<Map<String, BreedingSpeciesPresets.SpeciesPreset>> getSpeciesPresets() {
        return ApiResponse.success(presets.getPresets());
    }

    @PostMapping("/batch-start")
    public ApiResponse<String> batchStart(
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> tankIds = (List<String>) body.get("tankIds");
        String species = (String) body.getOrDefault("species", "guppy");

        if (tankIds == null || tankIds.isEmpty()) {
            return ApiResponse.error(400, "tankIds cannot be empty");
        }

        for (String tankId : tankIds) {
            breedingService.startBreeding(tankId, species);
        }

        return ApiResponse.success("Started breeding mode for " + tankIds.size() + " tanks");
    }

    @PostMapping("/batch-stop")
    public ApiResponse<String> batchStop(@RequestBody List<String> tankIds) {
        for (String tankId : tankIds) {
            breedingService.stopBreeding(tankId);
        }
        return ApiResponse.success("Stopped breeding mode for " + tankIds.size() + " tanks");
    }
}
