#include <PubSubClient.h>
#include <Wire.h>
#include <WiFiClient.h>
#include <LIS3DHTR.h>
#include <ArduinoJson.h>
#include <rpcWiFi.h>
#include "config.h"
#include "mqtt_topic.h"
#include "SerialTransfer.h"


const char *MQTT_CLIENT_ID_PREFIX = "hand-transmitter";


enum AccelerationDirection : uint8_t {
  NEUTRAL = 0,
  FORWARD = 1,
  REVERSE = 2
};

enum SteeringDirection : uint8_t {
  STRAIGHT = 0,
  LEFT = 1,
  RIGHT = 2
};

struct CarControlMessage {
  AccelerationDirection accelerationDirection;
  uint8_t accelerationAmount;
  SteeringDirection steeringDirection;
  uint8_t steeringAmount;
};

LIS3DHTR<TwoWire> lis;
WiFiClient wifiClient;
PubSubClient mqttClient(wifiClient);
StaticJsonDocument<200> jsonDoc;


float x_values, y_values, z_values;

//These constants are specified for maintainability reasons. They were achieved through testing
const float NEGATIVE_THREASHHOLD = -0.10;
const float POSITIVE_THREASHHOLD = 0.10;




void setup() {
  Serial.begin(9600);  // Start the serial communication
  Serial.println("test ");
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  while (!Serial)
    ;  // Wait for serial to be ready

  lis.begin(Wire1);  // Initialize the LIS3DHTR accelerometer
  if (!lis) {
    Serial.println("ERROR");
    while (1)
      ;
  }
  lis.setOutputDataRate(LIS3DHTR_DATARATE_25HZ);  // Set the data output rate
  lis.setFullScaleRange(LIS3DHTR_RANGE_2G);       // Set the scale range to 2g

  mqttClient.setServer(MQTT_BROKER_ADDRESS, MQTT_BROKER_PORT);

}



void loop() {
  // Read accelerometer values
  x_values = lis.getAccelerationX();
  y_values = lis.getAccelerationY();

  // Create a CarControlMessage and fill in the values
  CarControlMessage message;
  if (x_values > POSITIVE_THREASHHOLD) {
    message.accelerationDirection = FORWARD;
    message.accelerationAmount = x_values * 100;
  } else if (x_values < NEGATIVE_THREASHHOLD) {
    message.accelerationDirection = REVERSE;
    message.accelerationAmount = x_values * 100;
  } else {
    message.accelerationDirection = NEUTRAL;
    message.accelerationAmount = 0;
  }

  if (y_values > POSITIVE_THREASHHOLD) {
    message.steeringDirection = LEFT;
    message.steeringAmount = y_values * 100;
  } else if (y_values < NEGATIVE_THREASHHOLD) {
    message.steeringDirection = RIGHT;
    message.steeringAmount = -y_values * 100;
  } else {
    message.steeringDirection = STRAIGHT;
    message.steeringAmount = 0;
  }

  // Serialize the message struct to a byte array
  char*messageBytes[sizeof(message)];
  memcpy(messageBytes, &message, sizeof(message));

  while (!mqttClient.connected()) {
    String clientId = MQTT_CLIENT_ID_PREFIX + String(random(0xFFFF));
      mqttClient.connect(clientId.c_str(), MQTT_USERNAME, MQTT_PASSWORD);


      Serial.print("Connected: ");
      Serial.println(mqttClient.connected());
      Serial.print("State: ");
      Serial.println(mqttClient.state());

      if(!mqttClient.connected()){
              delay(5000);
      }
  }

  mqttClient.loop();

  if (mqttClient.connected()) {
    Serial.println("print line");
    mqttClient.publish(CAR_CONTROL_TOPIC_STRING, (const char*)messageBytes);

  }
}
