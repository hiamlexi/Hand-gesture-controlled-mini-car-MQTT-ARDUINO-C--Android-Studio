package edu.gu.dit133.group7.scout.mqtt.messages;

public enum SteeringDirection {
    STRAIGHT(0),
    LEFT(1),
    RIGHT(2);

    private final byte value;

    SteeringDirection(int value) {
        this.value = (byte) value;
    }

    public byte getValue() {
        return value;
    }
}
