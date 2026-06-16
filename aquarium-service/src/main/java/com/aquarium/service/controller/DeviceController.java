package com.aquarium.service.controller;

import com.aquarium.common.dto.ApiResponse;
import com.aquarium.common.dto.DeviceCommand;
import com.aquarium.common.enums.DeviceType;
import com.aquarium.service.entity.Device;
import com.aquarium.service.mapper.DeviceMapper;
import com.aquarium.service.tcp.handler.SensorDataHandler;
import com.aquarium.service.tcp.protocol.TcpMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceMapper deviceMapper;
    private final RabbitTemplate rabbitTemplate;

    private static final String DEVICE_COMMAND_EXCHANGE = "aquarium.device.command";

    @GetMapping("/tank/{tankId}")
    public ApiResponse<List<Device>> getDevicesByTank(@PathVariable String tankId) {
        List<Device> devices = deviceMapper.findByTankId(tankId);
        return ApiResponse.success(devices);
    }

    @GetMapping("/{deviceId}")
    public ApiResponse<Device> getDevice(@PathVariable String deviceId) {
        Device device = deviceMapper.findByDeviceId(deviceId);
        if (device == null) {
            return ApiResponse.error(404, "Device not found: " + deviceId);
        }
        return ApiResponse.success(device);
    }

    @PostMapping("/{deviceId}/command")
    public ApiResponse<String> sendCommand(
            @PathVariable String deviceId,
            @RequestBody Map<String, Object> body) {

        Device device = deviceMapper.findByDeviceId(deviceId);
        if (device == null) {
            return ApiResponse.error(404, "Device not found: " + deviceId);
        }

        String action = (String) body.getOrDefault("action", "toggle");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) body.getOrDefault("params", Map.of());

        DeviceCommand command = DeviceCommand.builder()
                .commandId(UUID.randomUUID().toString())
                .tankId(device.getTankId())
                .deviceType(DeviceType.fromCode(device.getDeviceType()))
                .action(action)
                .params(params)
                .issuedAt(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(DEVICE_COMMAND_EXCHANGE,
                "device.command." + command.getDeviceType().getCode(), command);

        sendTcpCommand(deviceId, command);

        log.info("Manual command sent: deviceId={}, type={}, action={}",
                deviceId, command.getDeviceType(), action);

        return ApiResponse.success("Command sent: " + command.getCommandId());
    }

    @PostMapping("/{deviceId}/toggle")
    public ApiResponse<String> toggleDevice(
            @PathVariable String deviceId,
            @RequestParam String action) {

        Device device = deviceMapper.findByDeviceId(deviceId);
        if (device == null) {
            return ApiResponse.error(404, "Device not found: " + deviceId);
        }

        DeviceCommand command = DeviceCommand.builder()
                .commandId(UUID.randomUUID().toString())
                .tankId(device.getTankId())
                .deviceType(DeviceType.fromCode(device.getDeviceType()))
                .action(action)
                .params(Map.of())
                .issuedAt(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(DEVICE_COMMAND_EXCHANGE,
                "device.command." + command.getDeviceType().getCode(), command);

        sendTcpCommand(deviceId, command);

        String newStatus = "on".equals(action) ? "on" : "off";
        deviceMapper.updateStatus(deviceId, newStatus, LocalDateTime.now());

        log.info("Device toggled: deviceId={}, newStatus={}", deviceId, newStatus);
        return ApiResponse.success("Device " + deviceId + " turned " + newStatus);
    }

    private void sendTcpCommand(String deviceId, DeviceCommand command) {
        io.netty.channel.ChannelHandlerContext ctx = SensorDataHandler.getDeviceChannel(deviceId);
        if (ctx != null && ctx.channel().isActive()) {
            try {
                String payload = String.format(
                        "{\"commandId\":\"%s\",\"deviceType\":\"%s\",\"action\":\"%s\"}",
                        command.getCommandId(),
                        command.getDeviceType().getCode(),
                        command.getAction());

                TcpMessage msg = TcpMessage.builder()
                        .magic(TcpMessage.MAGIC)
                        .type(TcpMessage.MessageType.DEVICE_STATUS.getCode())
                        .deviceId(Short.parseShort(deviceId))
                        .payload(payload)
                        .build();

                ctx.writeAndFlush(msg);
                log.debug("TCP command sent directly to device {}", deviceId);
            } catch (Exception e) {
                log.error("Failed to send TCP command to device {}", deviceId, e);
            }
        } else {
            log.warn("Device {} not connected via TCP, command queued via RabbitMQ", deviceId);
        }
    }
}
