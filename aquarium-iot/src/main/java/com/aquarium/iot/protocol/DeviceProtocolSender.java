package com.aquarium.iot.protocol;

import com.aquarium.common.enums.DeviceType;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class DeviceProtocolSender {

    @Value("${aquarium-service.host:localhost}")
    private String serviceHost;

    @Value("${aquarium-service.port:8080}")
    private int servicePort;

    private final EventLoopGroup workerGroup = new NioEventLoopGroup(2);

    public void sendToDevice(String tankId, DeviceType deviceType, String command) {
        log.info("Sending protocol command to device: tankId={}, type={}, cmd={}",
                tankId, deviceType, command);

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                                    byte[] bytes = new byte[msg.readableBytes()];
                                    msg.readBytes(bytes);
                                    String response = new String(bytes, StandardCharsets.UTF_8);
                                    log.debug("Device response: {}", response);
                                    ctx.close();
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                    log.error("Device protocol error", cause);
                                    ctx.close();
                                }
                            });
                        }
                    });

            ChannelFuture future = bootstrap.connect(serviceHost, 9527).sync();
            if (future.isSuccess()) {
                ByteBuf buf = future.channel().alloc().buffer();
                buf.writeByte(0x5A);
                String payload = String.format(
                        "{\"tankId\":\"%s\",\"deviceType\":\"%s\",\"command\":\"%s\"}",
                        tankId, deviceType.getCode(), command);
                byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
                buf.writeInt(12 + payloadBytes.length);
                buf.writeByte(0x04);
                buf.writeByte(0x01);
                buf.writeShort(extractDeviceNum(tankId));
                buf.writeByte(0x00);
                buf.writeBytes(payloadBytes);
                future.channel().writeAndFlush(buf);
                log.debug("Protocol command sent via TCP to device for tank {}", tankId);
            } else {
                log.warn("Failed to connect to TCP gateway for tank {}", tankId);
                logFallback(tankId, deviceType, command);
            }
        } catch (Exception e) {
            log.error("Failed to send protocol command to device: tankId={}", tankId, e);
            logFallback(tankId, deviceType, command);
        }
    }

    private void logFallback(String tankId, DeviceType deviceType, String command) {
        log.info("[FALLBACK] Protocol command logged for manual dispatch: tankId={}, type={}, cmd={}",
                tankId, deviceType, command);
    }

    private short extractDeviceNum(String tankId) {
        try {
            String num = tankId.replaceAll("[^0-9]", "");
            return Short.parseShort(num.isEmpty() ? "1" : num);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
