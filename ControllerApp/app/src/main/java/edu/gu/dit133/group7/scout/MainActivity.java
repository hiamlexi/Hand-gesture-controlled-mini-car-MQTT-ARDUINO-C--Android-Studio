package edu.gu.dit133.group7.scout;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.github.niqdev.mjpeg.DisplayMode;
import com.github.niqdev.mjpeg.Mjpeg;
import com.github.niqdev.mjpeg.MjpegSurfaceView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.gu.dit133.group7.scout.controls.VirtualJoystickView;
import edu.gu.dit133.group7.scout.mqtt.MqttClientFacade;
import edu.gu.dit133.group7.scout.mqtt.messages.CameraAddressMessage;
import edu.gu.dit133.group7.scout.mqtt.messages.CarControlMessage;

public class MainActivity extends AppCompatActivity {

    private static final String CAR_CONTROL_TOPIC = "car/control";
    private static final String CAMERA_CONTROL_TOPIC = "camera/control";
    private static final String CAMERA_ADDRESS_TOPIC = "camera/address";
    private static final String CAR_COLLISION_TOPIC = "car/collision";

    private static final int CAMERA_CONNECTION_TIMEOUT_SECONDS = 5; //seconds

    private static final byte CAMERA_LEFT = 1;
    private static final byte CAMERA_RIGHT = 2;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private MqttClientFacade mqttClientFacade;

    private MjpegSurfaceView mjpegView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mjpegView = findViewById(R.id.camera_view);

        setupCarController();
        setUpCameraController();

        mqttClientFacade = new MqttClientFacade(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    private void setupAlerts(SharedPreferences defaultSharedPreferences) {
        //initializing the invasive version of the alert
        AlertDialog.Builder collisionAlertBuilder = new AlertDialog.Builder(this);
        collisionAlertBuilder.setMessage("The car has stopped to prevent a collision. Tap anywhere to continue.");
        collisionAlertBuilder.setCancelable(true);

        collisionAlertBuilder.setPositiveButton(
                "Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }

                });

        //initializing the popup version of the alert
        Snackbar snackbar = Snackbar.make(mjpegView, "The car has stopped to prevent a collision.", Snackbar.LENGTH_LONG);

        boolean invasiveAlerts = defaultSharedPreferences.getBoolean("collisionAlert", false);

        mqttClientFacade.subscribe(CAR_COLLISION_TOPIC, (topic, message) -> {
            Log.i("app", "Received message on collision");
            if (invasiveAlerts) {
                Log.i("collisionAlert", "collisionAlert");
                MainActivity.this.runOnUiThread(() -> {
                    AlertDialog collisionAlert = collisionAlertBuilder.create();
                    collisionAlert.show();
                });
            } else {
                Log.i("snackbar", "snackbar");
                snackbar.show();
            }
        });
    }

    private void setupCarController() {
        VirtualJoystickView virtualJoystickView = findViewById(R.id.car_joystick);
        virtualJoystickView.setListener((magnitude, angle) -> {
            CarControlMessage carControlMessage = CarControlMessage.fromPolarCoordinates(magnitude, angle);
            mqttClientFacade.publish(CAR_CONTROL_TOPIC, carControlMessage.asByteArray());

        });
    }

    private void setUpCameraController() {
        FloatingActionButton cameraLeft = findViewById(R.id.camera_left);
        FloatingActionButton cameraRight = findViewById(R.id.camera_right);

        //User click on the button to control the direction of the camera
        cameraLeft.setOnClickListener(v -> mqttClientFacade.publish(CAMERA_CONTROL_TOPIC, CAMERA_LEFT));
        //User presses and holds the button to control the direction of the camera without multiple clicks
        cameraLeft.setOnLongClickListener(v -> {
            mqttClientFacade.publish(CAMERA_CONTROL_TOPIC, CAMERA_LEFT);
            return false;
        });

        cameraRight.setOnClickListener(v -> mqttClientFacade.publish(CAMERA_CONTROL_TOPIC, CAMERA_RIGHT));
        cameraRight.setOnLongClickListener(v -> {
            mqttClientFacade.publish(CAMERA_CONTROL_TOPIC, CAMERA_RIGHT);
            return false;
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        startActivity(new Intent(this, SettingsActivity.class));
        return true;
    }

    private void setupLiveVideoFeed(String cameraIp) {
        if (cameraIp == null || mjpegView.isStreaming()) {
            return;
        }

        Uri uri = Uri.parse(String.format("http://%s:81/stream", cameraIp));

        Mjpeg.newInstance()
                .open(uri.toString(), CAMERA_CONNECTION_TIMEOUT_SECONDS)
                .subscribe(inputStream -> {
                    mjpegView.setSource(inputStream);
                    mjpegView.setDisplayMode(DisplayMode.BEST_FIT);
                }, e -> Log.i("app", "Failed to connect live feed: " + e.getMessage(), e));
    }

    @Override
    protected void onPause() {
        super.onPause();

        mqttClientFacade.unsubscribe(CAMERA_ADDRESS_TOPIC);
        mqttClientFacade.unsubscribe(CAR_COLLISION_TOPIC);

        // Run on separate thread, otherwise android throws an exception due to networking on main thread
        executorService.submit(mjpegView::stopPlayback);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        updateSettings(defaultSharedPreferences);
        setupCamera(defaultSharedPreferences);
        setupAlerts(defaultSharedPreferences);
        mqttClientFacade.ensureMqttConnection(defaultSharedPreferences);
    }

    private void updateSettings(SharedPreferences defaultSharedPreferences) {
        boolean hideController = defaultSharedPreferences.getBoolean("controller", false);

        VirtualJoystickView virtualJoystickView = findViewById(R.id.car_joystick);
        if (hideController) {
            virtualJoystickView.setVisibility(View.INVISIBLE);
        } else {
            virtualJoystickView.setVisibility(View.VISIBLE);
        }

        virtualJoystickView.setDeadzoneAngle(defaultSharedPreferences.getInt("deadzone_angle", 15));
        virtualJoystickView.setDeadzoneRadius(defaultSharedPreferences.getInt("deadzone_radius", 20) / 100.0);
    }

    private void setupCamera(SharedPreferences defaultSharedPreferences) {
        // As defined in the issue, we want the user defined ip to take precedence. If it is
        // not defined, then we subscribe to the topic, and setup the camera view when
        // a message is received

        String configuredCameraIp = defaultSharedPreferences.getString("CameraIP", "").trim();
        if (!configuredCameraIp.isEmpty()) {
            setupLiveVideoFeed(configuredCameraIp);
        } else {
            mqttClientFacade.subscribe(CAMERA_ADDRESS_TOPIC, (topic, message) -> {
                CameraAddressMessage cameraAddressMessage = CameraAddressMessage.fromPayload(message.getPayload());
                if (cameraAddressMessage != null) {
                    // NOTE: We don't necessarily want to unsubscribe from the topic here,
                    // in case the camera disconnects for a while, is replaced by a
                    // different one with a new ip etc.
                    setupLiveVideoFeed(cameraAddressMessage.getIp());
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mqttClientFacade.disconnect();
    }
}