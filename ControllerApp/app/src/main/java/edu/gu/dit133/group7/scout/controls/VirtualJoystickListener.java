package edu.gu.dit133.group7.scout.controls;

public interface VirtualJoystickListener {

    /**
     * Called when the user changes the state of the joystick by interacting with it.
     *
     * @param magnitude distance of the control stick from the center, ranging from 0 to 1
     * @param angle     the angle of the rotation from the right, in radians
     */
    void onMoved(float magnitude, float angle);
}
