package com.aquarium.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("breeding_config")
public class BreedingConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tankId;
    private String species;
    private String modeStatus;
    private Double targetTemp;
    private Double targetPh;
    private String waterFlowLevel;
    private Boolean filterPumpEnabled;
    private String aerationLevel;
    private LocalDateTime startTime;
    private LocalDateTime expectedBirthTime;
    private LocalDateTime motherRemovalTime;
    private LocalDateTime frySafeTime;
    private Boolean autoStopFilter;
    private Boolean autoAdjustTemp;
    private Boolean reminderEnabled;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
