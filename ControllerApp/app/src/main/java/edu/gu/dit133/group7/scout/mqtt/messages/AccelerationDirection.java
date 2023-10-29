package edu.gu.dit133.group7.scout.mqtt.messages;

public enum AccelerationDirection {
    NEUTRAL(0),
    FORWARD(1),
    REVERSE(2);

    private final byte value;

    AccelerationDirection(int value) {
        this.value = (byte) value;
    }

    public byte getValue() {
        return value;
    }
}
