package edu.gu.dit133.group7.scout.mqtt.messages;

import androidx.annotation.Nullable;

public class CameraAddressMessage {

    @Nullable
    public static CameraAddressMessage fromPayload(byte[] payload) {
        if(payload.length < 4) {
            return null;
        }

        String ip = String.format("%d.%d.%d.%d",
                payload[0] & 0xFF,
                payload[1] & 0xFF,
                payload[2] & 0xFF,
                payload[3] & 0xFF);

        return new CameraAddressMessage(ip);
    }

    private final String ip;

    private CameraAddressMessage(String ip) {
        this.ip = ip;
    }

    public String getIp() {
        return ip;
    }
}
