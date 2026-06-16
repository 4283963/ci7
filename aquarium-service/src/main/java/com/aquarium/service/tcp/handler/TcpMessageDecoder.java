package com.aquarium.service.tcp.handler;

import com.aquarium.service.tcp.protocol.TcpMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
public class TcpMessageDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < TcpMessage.HEADER_LENGTH) {
            return;
        }

        in.markReaderIndex();

        byte magic = in.readByte();
        if (magic != TcpMessage.MAGIC) {
            log.warn("Invalid magic byte: 0x{}, closing connection", Integer.toHexString(magic & 0xFF));
            ctx.close();
            return;
        }

        int length = in.readInt();
        byte type = in.readByte();
        byte version = in.readByte();
        short deviceId = in.readShort();
        byte reserved = in.readByte();

        int payloadLength = length - TcpMessage.HEADER_LENGTH;
        if (payloadLength < 0) {
            log.warn("Invalid message length: {}", length);
            ctx.close();
            return;
        }

        if (in.readableBytes() < payloadLength) {
            in.resetReaderIndex();
            return;
        }

        String payload = "";
        if (payloadLength > 0) {
            byte[] payloadBytes = new byte[payloadLength];
            in.readBytes(payloadBytes);
            payload = new String(payloadBytes, StandardCharsets.UTF_8);
        }

        TcpMessage message = TcpMessage.builder()
                .magic(magic)
                .length(length)
                .type(type)
                .version(version)
                .deviceId(deviceId)
                .reserved(reserved)
                .payload(payload)
                .build();

        out.add(message);
    }
}
