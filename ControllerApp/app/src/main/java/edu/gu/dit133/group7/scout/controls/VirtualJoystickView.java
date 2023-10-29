package edu.gu.dit133.group7.scout.controls;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

public class VirtualJoystickView extends View implements View.OnTouchListener {

    private static final Paint BACKGROUND_CIRCLE_PAINT = new Paint();
    private static final Paint BORDER_PAINT = new Paint();
    private static final Paint GRID_PAINT = new Paint();
    private static final Paint CONTROL_CIRCLE_PAINT = new Paint();

    private static final float BORDER_WIDTH = 10.0f;
    private static final float GRID_LINE_WIDTH = 2.5f;
    private static final float CONTROL_CIRCLE_SIZE_RATIO = 0.125f;

    private static final double HALF_PI = Math.PI / 2.0;

    static {
        BACKGROUND_CIRCLE_PAINT.setColor(0xEEEEEE);
        BACKGROUND_CIRCLE_PAINT.setAlpha(0xFF);

        BORDER_PAINT.setStyle(Paint.Style.STROKE);
        BORDER_PAINT.setColor(0xAAAAAA);
        BORDER_PAINT.setAlpha(0xFF);
        BORDER_PAINT.setStrokeWidth(BORDER_WIDTH);

        GRID_PAINT.setStyle(Paint.Style.STROKE);
        GRID_PAINT.setColor(0xAAAAAA);
        GRID_PAINT.setAlpha(0xFF);
        GRID_PAINT.setStrokeWidth(GRID_LINE_WIDTH);

        CONTROL_CIRCLE_PAINT.setColor(0xBBBBBB);
        CONTROL_CIRCLE_PAINT.setAlpha(0xFF);
    }

    private float centerX;
    private float centerY;
    private float radius;
    private float smallCircleRadius;

    private float controlX;
    private float controlY;

    private double deadzoneAngle = Math.toRadians(15.0f);
    private double deadzoneRadius = 0.2f;

    @NonNull
    private VirtualJoystickListener virtualJoystickListener = (magnitude, angle) -> {
    };

    public VirtualJoystickView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VirtualJoystickView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setOnTouchListener(this);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        this.radius = Math.min(getWidth(), getHeight()) / 2.75f;
        this.centerX = getWidth() / 2.0f;
        this.centerY = getHeight() / 2.0f;
        this.smallCircleRadius = Math.min(getWidth(), getHeight()) * CONTROL_CIRCLE_SIZE_RATIO;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Draw outer circle and the border
        canvas.drawCircle(centerX, centerY, radius - BORDER_WIDTH, BACKGROUND_CIRCLE_PAINT);
        canvas.drawCircle(centerX, centerY, radius - BORDER_WIDTH, BORDER_PAINT);

        // Draw grid
        canvas.drawLine(centerX, centerY - radius + BORDER_WIDTH, centerX, centerY + radius - BORDER_WIDTH, GRID_PAINT);
        canvas.drawLine(centerX - radius + BORDER_WIDTH, centerY, centerX + radius - BORDER_WIDTH, centerY, GRID_PAINT);

        // Draw the inner control circle
        canvas.drawCircle(centerX + controlX, centerY + controlY, smallCircleRadius, CONTROL_CIRCLE_PAINT);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v != this) {
            return false;
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            controlX = 0;
            controlY = 0;
            virtualJoystickListener.onMoved(0.0f, 0.0f);
        } else {
            controlX = event.getX() - centerX;
            controlY = event.getY() - centerY;

            float distance = (float) Math.sqrt(controlX * controlX + controlY * controlY);
            float magnitude = distance / radius;

            // invert Y because the coordinate system is +Y down
            float angle = (float) Math.atan2(-controlY, controlX);

            if (magnitude > 1.0f) {
                controlX = controlX / distance * radius;
                controlY = controlY / distance * radius;
                magnitude = 1.0f;
            } else if (magnitude <= deadzoneRadius) {
                magnitude = 0.0f;
                angle = (float) HALF_PI;
            } else {
                magnitude = (float) ((magnitude - deadzoneRadius) * (1.0 / (1.0 - deadzoneRadius)));
            }

            invokeListener(magnitude, angle);
        }

        // Forces the view to redraw
        invalidate();

        return true;
    }

    private void invokeListener(float magnitude, float angle) {
        // Map angle to [0, 2PI)
        angle = (float) ((angle + Math.PI * 2.0) % (Math.PI * 2.0));

        if (!isAngleWithinAngularDeadzone(angle)) {
            virtualJoystickListener.onMoved(magnitude, (float) mapAngleBetweenAngularDeadzones(angle));
        } else if (Math.abs(Math.cos(angle)) < 0.5) {
            // Either at the top or bottom angular deadzone
            virtualJoystickListener.onMoved(magnitude, (float) (HALF_PI * Math.round(angle / HALF_PI)));
        } else if (Math.cos(angle) >= 0) {
            // On the right angular deadzone
            virtualJoystickListener.onMoved(magnitude, 0.0f);
        } else {
            // On the left angular deadzone.
            virtualJoystickListener.onMoved(magnitude, (float) Math.PI);
        }
    }

    private boolean isAngleWithinAngularDeadzone(double angle) {
        double remainder = angle % HALF_PI;
        return remainder <= deadzoneAngle || Math.abs(remainder - HALF_PI) <= deadzoneAngle;
    }

    /**
     * Maps the given angle between two angular deadzones (straight up or down, and straight right
     * or left) so that values range between 0% to 100% instead of whatever the deadzone would
     * normally offset.
     * @param angle The angle in radians, in the range of 0 inclusive, to 2PI exclusive
     * @return The mapped angle
     */
    private double mapAngleBetweenAngularDeadzones(double angle) {
        double quadrant = (angle % HALF_PI) - deadzoneAngle;
        quadrant = (quadrant / (HALF_PI - deadzoneAngle * 2)) * (HALF_PI);
        return Math.floor(angle / (HALF_PI)) * HALF_PI + quadrant;
    }

    public void setListener(@NonNull VirtualJoystickListener virtualJoystickListener) {
        this.virtualJoystickListener = Objects.requireNonNull(virtualJoystickListener);
    }

    /**
     * @param deadzoneAngle angle, in degrees
     */
    public void setDeadzoneAngle(double deadzoneAngle) {
        this.deadzoneAngle = Math.toRadians(deadzoneAngle);
    }

    /**
     * @param deadzoneRadius percentage of the joysticks radius, ranging from 0.0 to 1.0
     */
    public void setDeadzoneRadius(double deadzoneRadius) {
        this.deadzoneRadius = deadzoneRadius;
    }
}
