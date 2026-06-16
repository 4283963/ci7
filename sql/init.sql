CREATE DATABASE IF NOT EXISTS smart_aquarium
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE smart_aquarium;

CREATE TABLE IF NOT EXISTS tank (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tank_id VARCHAR(64) NOT NULL UNIQUE COMMENT '鱼缸编号',
    name VARCHAR(128) NOT NULL COMMENT '鱼缸名称',
    capacity DECIMAL(10,2) DEFAULT 0 COMMENT '容量(升)',
    location VARCHAR(256) DEFAULT NULL COMMENT '位置',
    status VARCHAR(32) DEFAULT 'normal' COMMENT '状态: normal/maintenance/offline',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tank_id (tank_id)
) ENGINE=InnoDB COMMENT='鱼缸信息表';

CREATE TABLE IF NOT EXISTS device (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(64) NOT NULL UNIQUE COMMENT '设备编号',
    tank_id VARCHAR(64) NOT NULL COMMENT '所属鱼缸',
    device_type VARCHAR(32) NOT NULL COMMENT '设备类型: heater/chlorine_valve/water_pump/aeration_pump',
    status VARCHAR(16) DEFAULT 'off' COMMENT '设备状态: on/off/error/offline',
    last_heartbeat DATETIME DEFAULT NULL COMMENT '最后心跳时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tank_id (tank_id),
    INDEX idx_device_type (device_type)
) ENGINE=InnoDB COMMENT='设备信息表';

CREATE TABLE IF NOT EXISTS water_quality_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tank_id VARCHAR(64) NOT NULL COMMENT '鱼缸编号',
    device_id VARCHAR(64) DEFAULT NULL COMMENT '传感器设备编号',
    temperature DECIMAL(5,2) DEFAULT NULL COMMENT '水温(℃)',
    ph DECIMAL(4,2) DEFAULT NULL COMMENT 'pH值',
    chlorine DECIMAL(8,4) DEFAULT NULL COMMENT '残余氯气(mg/L)',
    dissolved_oxygen DECIMAL(5,2) DEFAULT NULL COMMENT '溶氧量(mg/L)',
    recorded_at DATETIME NOT NULL COMMENT '记录时间',
    INDEX idx_tank_time (tank_id, recorded_at),
    INDEX idx_recorded_at (recorded_at)
) ENGINE=InnoDB COMMENT='水质监测记录表';

CREATE TABLE IF NOT EXISTS alert_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_id VARCHAR(64) NOT NULL UNIQUE COMMENT '告警编号',
    tank_id VARCHAR(64) NOT NULL COMMENT '鱼缸编号',
    metric VARCHAR(32) NOT NULL COMMENT '指标: temperature/ph/chlorine/dissolved_oxygen',
    actual_value DECIMAL(10,4) DEFAULT NULL COMMENT '实际值',
    threshold_min DECIMAL(10,4) DEFAULT NULL COMMENT '阈值下限',
    threshold_max DECIMAL(10,4) DEFAULT NULL COMMENT '阈值上限',
    level VARCHAR(16) NOT NULL COMMENT '告警级别: info/warning/critical/fatal',
    message VARCHAR(512) DEFAULT NULL COMMENT '告警消息',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tank_level (tank_id, level),
    INDEX idx_created_at (created_at),
    INDEX idx_level (level)
) ENGINE=InnoDB COMMENT='告警记录表';

CREATE TABLE IF NOT EXISTS device_command_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    command_id VARCHAR(64) NOT NULL UNIQUE COMMENT '指令编号',
    tank_id VARCHAR(64) NOT NULL COMMENT '鱼缸编号',
    device_type VARCHAR(32) NOT NULL COMMENT '设备类型',
    action VARCHAR(32) NOT NULL COMMENT '动作: on/off/open/close/set_temp',
    params TEXT DEFAULT NULL COMMENT '指令参数(JSON)',
    status VARCHAR(16) DEFAULT 'RECEIVED' COMMENT '状态: RECEIVED/SUCCESS/FAILED',
    result TEXT DEFAULT NULL COMMENT '执行结果',
    issued_at DATETIME DEFAULT NULL COMMENT '下发时间',
    executed_at DATETIME DEFAULT NULL COMMENT '执行时间',
    INDEX idx_tank (tank_id),
    INDEX idx_status (status),
    INDEX idx_issued (issued_at)
) ENGINE=InnoDB COMMENT='设备指令日志表';

INSERT INTO tank (tank_id, name, capacity, location, status) VALUES
('TANK-1', '热带鱼展示缸', 500.00, 'A区-1号位', 'normal'),
('TANK-2', '珊瑚礁生态缸', 300.00, 'A区-2号位', 'normal'),
('TANK-3', '淡水观赏缸', 200.00, 'B区-1号位', 'normal');

INSERT INTO device (device_id, tank_id, device_type, status) VALUES
('DEV-001', 'TANK-1', 'heater', 'off'),
('DEV-002', 'TANK-1', 'chlorine_valve', 'off'),
('DEV-003', 'TANK-1', 'water_pump', 'off'),
('DEV-004', 'TANK-1', 'aeration_pump', 'off'),
('DEV-005', 'TANK-2', 'heater', 'off'),
('DEV-006', 'TANK-2', 'chlorine_valve', 'off'),
('DEV-007', 'TANK-2', 'water_pump', 'off'),
('DEV-008', 'TANK-2', 'aeration_pump', 'off'),
('DEV-009', 'TANK-3', 'heater', 'off'),
('DEV-010', 'TANK-3', 'chlorine_valve', 'off'),
('DEV-011', 'TANK-3', 'water_pump', 'off'),
('DEV-012', 'TANK-3', 'aeration_pump', 'off');

CREATE TABLE IF NOT EXISTS breeding_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tank_id VARCHAR(64) NOT NULL UNIQUE COMMENT '鱼缸编号',
    species VARCHAR(64) DEFAULT 'guppy' COMMENT '鱼种类: guppy/betta/molly/platy',
    mode_status VARCHAR(16) DEFAULT 'idle' COMMENT '模式: idle/preparing/breeding/hatching',
    target_temp DECIMAL(5,2) DEFAULT 26.5 COMMENT '目标水温(℃)',
    target_ph DECIMAL(4,2) DEFAULT 7.2 COMMENT '目标pH值',
    water_flow_level VARCHAR(16) DEFAULT 'low' COMMENT '水流强度: low/medium/high',
    filter_pump_enabled TINYINT(1) DEFAULT 0 COMMENT '过滤泵开关(繁育期关闭防吸苗)',
    aeration_level VARCHAR(16) DEFAULT 'medium' COMMENT '增氧强度: low/medium/high',
    start_time DATETIME DEFAULT NULL COMMENT '繁育开始时间',
    expected_birth_time DATETIME DEFAULT NULL COMMENT '预计产仔时间',
    mother_removal_time DATETIME DEFAULT NULL COMMENT '母鱼捞取时间',
    fry_safe_time DATETIME DEFAULT NULL COMMENT '鱼苗安全时间',
    auto_stop_filter TINYINT(1) DEFAULT 1 COMMENT '是否自动停过滤泵',
    auto_adjust_temp TINYINT(1) DEFAULT 1 COMMENT '是否自动调温',
    reminder_enabled TINYINT(1) DEFAULT 1 COMMENT '是否开启提醒',
    note VARCHAR(512) DEFAULT NULL COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tank_id (tank_id),
    INDEX idx_mode_status (mode_status)
) ENGINE=InnoDB COMMENT='繁育模式配置表';

INSERT INTO breeding_config
    (tank_id, species, mode_status, target_temp, target_ph, water_flow_level,
     filter_pump_enabled, aeration_level, auto_stop_filter, auto_adjust_temp, reminder_enabled)
VALUES
    ('TANK-1', 'guppy', 'idle', 26.5, 7.2, 'low', 1, 'medium', 1, 1, 1),
    ('TANK-2', 'guppy', 'idle', 26.5, 7.2, 'low', 1, 'medium', 1, 1, 1),
    ('TANK-3', 'guppy', 'idle', 26.5, 7.2, 'low', 1, 'medium', 1, 1, 1);
