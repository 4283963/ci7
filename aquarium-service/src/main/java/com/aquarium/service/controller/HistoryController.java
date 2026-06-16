package com.aquarium.service.controller;

import com.aquarium.common.dto.ApiResponse;
import com.aquarium.service.entity.WaterQualityRecord;
import com.aquarium.service.mapper.WaterQualityRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final WaterQualityRecordMapper recordMapper;

    @GetMapping("/tank/{tankId}")
    public ApiResponse<List<WaterQualityRecord>> getHistory(
            @PathVariable String tankId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "100") int limit) {

        if (startTime == null) {
            startTime = LocalDateTime.now().minusHours(24);
        }
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }

        List<WaterQualityRecord> records = recordMapper.findByTankIdAndTimeRange(tankId, startTime, endTime);
        return ApiResponse.success(records);
    }

    @GetMapping("/tank/{tankId}/latest")
    public ApiResponse<List<WaterQualityRecord>> getLatest(
            @PathVariable String tankId,
            @RequestParam(defaultValue = "20") int limit) {
        List<WaterQualityRecord> records = recordMapper.findLatestByTankId(tankId, limit);
        return ApiResponse.success(records);
    }
}
