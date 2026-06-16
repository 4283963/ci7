package com.aquarium.service.tcp.handler;

import com.aquarium.service.tcp.protocol.TcpMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
public class TcpMessageEncoder extends MessageToByteEncoder<TcpMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, TcpMessage msg, ByteBuf out) {
        byte[] payloadBytes = msg.getPayload() != null
                ? msg.getPayload().getBytes(StandardCharsets.UTF_8)
                : new byte[0];

        int totalLength = TcpMessage.HEADER_LENGTH + payloadBytes.length;

        out.writeByte(msg.getMagic() != 0 ? msg.getMagic() : TcpMessage.MAGIC);
        out.writeInt(totalLength);
        out.writeByte(msg.getType());
        out.writeByte(msg.getVersion() != 0 ? msg.getVersion() : 1);
        out.writeShort(msg.getDeviceId());
        out.writeByte(msg.getReserved());

        if (payloadBytes.length > 0) {
            out.writeBytes(payloadBytes);
        }
    }
}
