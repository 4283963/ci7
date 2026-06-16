package com.aquarium.service.tcp.protocol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TcpMessage {

    public static final byte MAGIC = 0x5A;
    public static final int HEADER_LENGTH = 12;

    private byte magic;
    private int length;
    private byte type;
    private byte version;
    private short deviceId;
    private byte reserved;
    private String payload;

    public enum MessageType {
        HEARTBEAT((byte) 0x01),
        SENSOR_DATA((byte) 0x02),
        COMMAND_ACK((byte) 0x03),
        DEVICE_STATUS((byte) 0x04),
        ERROR((byte) 0xFF);

        private final byte code;

        MessageType(byte code) {
            this.code = code;
        }

        public byte getCode() {
            return code;
        }

        public static MessageType fromCode(byte code) {
            for (MessageType type : values()) {
                if (type.code == code) {
                    return type;
                }
            }
            return ERROR;
        }
    }
}
