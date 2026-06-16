package com.aquarium.service.controller;

import com.aquarium.common.dto.ApiResponse;
import com.aquarium.common.dto.SensorData;
import com.aquarium.service.water.WaterQualityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/water-quality")
@RequiredArgsConstructor
public class WaterQualityController {

    private final WaterQualityService waterQualityService;

    @GetMapping("/latest/{tankId}")
    public ApiResponse<SensorData> getLatestData(@PathVariable String tankId) {
        SensorData data = waterQualityService.getLatestData(tankId);
        if (data == null) {
            return ApiResponse.error(404, "No sensor data found for tank: " + tankId);
        }
        return ApiResponse.success(data);
    }

    @GetMapping("/latest")
    public ApiResponse<Map<String, SensorData>> getAllLatestData() {
        return ApiResponse.success(waterQualityService.getAllLatestData());
    }
}
