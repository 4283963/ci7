package com.aquarium.service.controller;

import com.aquarium.common.dto.ApiResponse;
import com.aquarium.service.entity.AlertRecord;
import com.aquarium.service.mapper.AlertRecordMapper;
import com.aquarium.service.entity.WaterQualityRecord;
import com.aquarium.service.mapper.WaterQualityRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertRecordMapper alertRecordMapper;

    @GetMapping("/tank/{tankId}")
    public ApiResponse<List<AlertRecord>> getAlertsByTank(
            @PathVariable String tankId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        if (startTime == null) {
            startTime = LocalDateTime.now().minusHours(24);
        }
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }

        List<AlertRecord> alerts = alertRecordMapper.findByTankIdAndTimeRange(tankId, startTime, endTime);
        return ApiResponse.success(alerts);
    }

    @GetMapping("/critical/{tankId}")
    public ApiResponse<List<AlertRecord>> getCriticalAlerts(@PathVariable String tankId) {
        List<AlertRecord> alerts = alertRecordMapper.findCriticalByTankId(tankId, 50);
        return ApiResponse.success(alerts);
    }

    @GetMapping("/level/{level}")
    public ApiResponse<List<AlertRecord>> getAlertsByLevel(
            @PathVariable String level,
            @RequestParam(defaultValue = "100") int limit) {
        List<AlertRecord> alerts = alertRecordMapper.findByLevel(level, limit);
        return ApiResponse.success(alerts);
    }
}
