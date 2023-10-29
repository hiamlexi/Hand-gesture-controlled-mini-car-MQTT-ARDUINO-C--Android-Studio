package edu.gu.dit133.group7.scout.mqtt.messages;

import static edu.gu.dit133.group7.scout.mqtt.messages.AccelerationDirection.*;
import static edu.gu.dit133.group7.scout.mqtt.messages.SteeringDirection.*;

public class CarControlMessage {

    private static final int AMOUNT_MAX_VALUE = 0xFF;

    private static final double HALF_PI = Math.PI / 2.0;
    private static final double EPSILON = 0.001;

    public static CarControlMessage fromPolarCoordinates(float magnitude, float angle) {
        AccelerationDirection accelerationDirection = NEUTRAL;
        SteeringDirection steeringDirection = STRAIGHT;

        double sin = Math.sin(angle);
        double cos = Math.cos(angle);

        int accelerationAmount = (int) (magnitude * AMOUNT_MAX_VALUE);
        int steeringAmount = (int) ((HALF_PI - Math.acos(Math.abs(cos))) / HALF_PI * AMOUNT_MAX_VALUE);

        if (magnitude > 0.0f) {
            accelerationDirection = sin >= -EPSILON ? FORWARD : REVERSE;
        }

        if (Math.abs(cos) >= EPSILON) {
            steeringDirection = cos > 0.0f ? RIGHT : LEFT;
        }

        return new CarControlMessage(accelerationDirection, accelerationAmount, steeringDirection, steeringAmount);
    }

    private final AccelerationDirection accelerationDirection;
    private final int accelerationAmount;
    private final SteeringDirection steeringDirection;
    private final int steeringAmount;

    public CarControlMessage(AccelerationDirection accelerationDirection,
                             int accelerationAmount,
                             SteeringDirection steeringDirection,
                             int steeringAmount) {
        this.accelerationDirection = accelerationDirection;
        this.accelerationAmount = accelerationAmount;
        this.steeringDirection = steeringDirection;
        this.steeringAmount = steeringAmount;
    }

    public AccelerationDirection getAccelerationDirection() {
        return accelerationDirection;
    }

    public int getAccelerationAmount() {
        return accelerationAmount;
    }

    public SteeringDirection getSteeringDirection() {
        return steeringDirection;
    }

    public int getSteeringAmount() {
        return steeringAmount;
    }

    public byte[] asByteArray() {
        return new byte[]{
                accelerationDirection.getValue(),
                (byte) accelerationAmount,
                steeringDirection.getValue(),
                (byte) steeringAmount
        };
    }
}
