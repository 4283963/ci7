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
@TableName("water_quality_record")
public class WaterQualityRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tankId;
    private String deviceId;
    private Double temperature;
    private Double ph;
    private Double chlorine;
    private Double dissolvedOxygen;
    private LocalDateTime recordedAt;
}
