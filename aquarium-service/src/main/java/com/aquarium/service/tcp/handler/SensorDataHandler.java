package com.aquarium.service.tcp.handler;

import com.aquarium.common.dto.SensorData;
import com.aquarium.service.tcp.protocol.TcpMessage;
import com.aquarium.service.water.WaterQualityService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class SensorDataHandler extends SimpleChannelInboundHandler<TcpMessage> {

    private final WaterQualityService waterQualityService;
    private final ObjectMapper objectMapper;

    private static final ConcurrentHashMap<String, ChannelHandlerContext> DEVICE_CHANNELS = new ConcurrentHashMap<>();

    public static ChannelHandlerContext getDeviceChannel(String deviceId) {
        return DEVICE_CHANNELS.get(deviceId);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("Device connected: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String deviceId = findDeviceIdByChannel(ctx);
        if (deviceId != null) {
            DEVICE_CHANNELS.remove(deviceId);
            log.info("Device disconnected: deviceId={}, address={}", deviceId, ctx.channel().remoteAddress());
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TcpMessage msg) {
        TcpMessage.MessageType type = TcpMessage.MessageType.fromCode(msg.getType());

        switch (type) {
            case HEARTBEAT -> handleHeartbeat(ctx, msg);
            case SENSOR_DATA -> handleSensorData(ctx, msg);
            case COMMAND_ACK -> handleCommandAck(ctx, msg);
            case DEVICE_STATUS -> handleDeviceStatus(ctx, msg);
            default -> log.warn("Unknown message type: 0x{}", Integer.toHexString(type.getCode() & 0xFF));
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            log.warn("Device idle timeout, closing: {}", ctx.channel().remoteAddress());
            ctx.close();
        }
    }

    private void handleHeartbeat(ChannelHandlerContext ctx, TcpMessage msg) {
        String deviceId = String.valueOf(msg.getDeviceId());
        DEVICE_CHANNELS.put(deviceId, ctx);

        TcpMessage ack = TcpMessage.builder()
                .magic(TcpMessage.MAGIC)
                .type(TcpMessage.MessageType.HEARTBEAT.getCode())
                .deviceId(msg.getDeviceId())
                .payload("ACK")
                .build();
        ctx.writeAndFlush(ack);
        log.debug("Heartbeat ACK sent to device {}", deviceId);
    }

    private void handleSensorData(ChannelHandlerContext ctx, TcpMessage msg) {
        try {
            String deviceId = String.valueOf(msg.getDeviceId());
            DEVICE_CHANNELS.put(deviceId, ctx);

            JsonNode root = objectMapper.readTree(msg.getPayload());
            SensorData sensorData = SensorData.builder()
                    .deviceId(deviceId)
                    .temperature(root.path("temperature").asDouble())
                    .ph(root.path("ph").asDouble())
                    .chlorine(root.path("chlorine").asDouble())
                    .dissolvedOxygen(root.path("dissolved_oxygen").asDouble())
                    .timestamp(LocalDateTime.now())
                    .build();

            waterQualityService.processSensorData(sensorData);

            TcpMessage ack = TcpMessage.builder()
                    .magic(TcpMessage.MAGIC)
                    .type(TcpMessage.MessageType.COMMAND_ACK.getCode())
                    .deviceId(msg.getDeviceId())
                    .payload("{\"status\":\"received\"}")
                    .build();
            ctx.writeAndFlush(ack);

        } catch (Exception e) {
            log.error("Failed to parse sensor data: {}", msg.getPayload(), e);
        }
    }

    private void handleCommandAck(ChannelHandlerContext ctx, TcpMessage msg) {
        log.info("Command ACK from device {}: {}", msg.getDeviceId(), msg.getPayload());
    }

    private void handleDeviceStatus(ChannelHandlerContext ctx, TcpMessage msg) {
        log.info("Device status report from {}: {}", msg.getDeviceId(), msg.getPayload());
    }

    private String findDeviceIdByChannel(ChannelHandlerContext ctx) {
        for (var entry : DEVICE_CHANNELS.entrySet()) {
            if (entry.getValue() == ctx) {
                return entry.getKey();
            }
        }
        return null;
    }
}
