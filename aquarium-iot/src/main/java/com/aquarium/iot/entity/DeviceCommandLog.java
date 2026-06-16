package com.aquarium.iot.entity;

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
@TableName("device_command_log")
public class DeviceCommandLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String commandId;
    private String tankId;
    private String deviceType;
    private String action;
    private String params;
    private String status;
    private String result;
    private LocalDateTime issuedAt;
    private LocalDateTime executedAt;
}
