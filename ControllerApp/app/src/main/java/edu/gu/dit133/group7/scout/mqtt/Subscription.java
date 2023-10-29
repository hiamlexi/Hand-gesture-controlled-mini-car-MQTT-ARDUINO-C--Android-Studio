package edu.gu.dit133.group7.scout.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;

public class Subscription {

    private final String topic;
    private final IMqttMessageListener messageListener;

    public Subscription(String topic, IMqttMessageListener messageListener) {
        this.topic = topic;
        this.messageListener = messageListener;
    }

    public String getTopic() {
        return topic;
    }

    public IMqttMessageListener getMessageListener() {
        return messageListener;
    }
}
