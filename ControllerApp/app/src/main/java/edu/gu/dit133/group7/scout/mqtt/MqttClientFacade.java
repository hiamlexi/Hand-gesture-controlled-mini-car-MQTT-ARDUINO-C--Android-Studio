package edu.gu.dit133.group7.scout.mqtt;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import java.util.ArrayList;
import java.util.List;

import info.mqtt.android.service.MqttAndroidClient;
import info.mqtt.android.service.QoS;

public class MqttClientFacade {

    private static final String MQTT_CLIENT_ID = "android_app_controller_" + (int) (Math.random() * 0xFFFF);
    private static final QoS QUALITY_OF_SERVICE = QoS.AtMostOnce;

    private final Context context;
    private final List<Subscription> subscriptions = new ArrayList<>();

    private MqttAndroidClient mqttAndroidClient = null;

    public MqttClientFacade(Context context) {
        this.context = context;
    }

    /**
     * Publish a message with a single byte value
     * @param topic     the topic to publish on
     * @param message   the byte value that is to be the entire message
     */
    public void publish(String topic, byte message) {
        publish(topic, new byte[]{message});
    }

    public void publish(String topic, byte[] message) {
        if (isConnected()) {
            mqttAndroidClient.publish(topic, message, QUALITY_OF_SERVICE.getValue(), false);
        }
    }

    public void subscribe(String topic, IMqttMessageListener messageListener) {
        if(isSubscribed(topic)) {
            throw new IllegalStateException("Already subscribed to " + topic);
        }

        subscriptions.add(new Subscription(topic, messageListener));

        if (isConnected()) {
            mqttAndroidClient.subscribe(topic, QUALITY_OF_SERVICE.getValue(), messageListener);
        }
    }

    private boolean isSubscribed(String topic) {
        return subscriptions.stream()
                .map(Subscription::getTopic)
                .anyMatch(topic::equals);
    }

    public void unsubscribe(String topic) {
        boolean wasRemoved = subscriptions.removeIf(e -> e.getTopic().equals(topic));
        if (wasRemoved && mqttAndroidClient != null) {
            mqttAndroidClient.unsubscribe(topic);
        }
    }

    private boolean isConnected() {
        return mqttAndroidClient != null && mqttAndroidClient.isConnected();
    }

    public void ensureMqttConnection(SharedPreferences preferences) {
        String brokerUrlFromSettings = preferences.getString("URL", null);

        if (brokerUrlFromSettings == null) {
            Log.e("app", "Broker URL is null!");
            return;
        }

        // We only want to connect or reconnect if we are not connected, or the broker address has changed
        if (!isConnected() || !mqttAndroidClient.getServerURI().equals(brokerUrlFromSettings)) {
            String username = preferences.getString("mqtt_username", null);
            String password = preferences.getString("mqtt_password", null);

            // If we're already connected, disconnect first. In this case we need to wait for the
            // broker to disconnect before connecting again, which is why we call from the callback
            if (mqttAndroidClient != null && mqttAndroidClient.isConnected()) {
                mqttAndroidClient.disconnect(null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        connectToMqtt(brokerUrlFromSettings, username, password);
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        connectToMqtt(brokerUrlFromSettings, username, password);
                    }
                });
            } else {
                // If we weren't connected and didn't need to wait to disconnect, we can just connect straight away
                connectToMqtt(brokerUrlFromSettings, username, password);
            }
        }
    }

    private void connectToMqtt(String brokerUrl, @Nullable String username, @Nullable String password) {
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);

        // If either username or password is null, we do not use any of those as per preference description
        if (username != null && password != null) {
            mqttConnectOptions.setUserName(username);
            mqttConnectOptions.setPassword(password.toCharArray());
        }

        mqttAndroidClient = new MqttAndroidClient(context, brokerUrl, MQTT_CLIENT_ID);
        mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                for (Subscription subscription : subscriptions) {
                    mqttAndroidClient.subscribe(subscription.getTopic(), QUALITY_OF_SERVICE.getValue(), subscription.getMessageListener());
                }
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

            }
        });
    }

    public void disconnect() {
        if (mqttAndroidClient != null) {
            mqttAndroidClient.disconnect();
        }
    }
}
