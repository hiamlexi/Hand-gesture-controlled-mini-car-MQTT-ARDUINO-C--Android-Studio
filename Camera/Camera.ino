#include "esp_camera.h"
#include <WiFi.h>
#include <WiFiClientSecure.h>

#include "config.h"
#include "mqtt_topic.h"

#include "PubSubClient.h"
#include "SerialTransfer.h"

#define CAMERA_MODEL_AI_THINKER 
#include "camera_pins.h"

#define CAMERA_ADDRESS_PUBLISH_INTERVAL_MS 2000

const char *MQTT_CLIENT_ID_PREFIX = "esp32-cam";

SerialTransfer serialTransfer;

WiFiClientSecure espClient;
PubSubClient client(espClient);

unsigned long lastTimeAddressPublished = 0;

void startCameraServer();
void setupLedFlash(int pin);

void setup() {
  Serial.begin(115200);
  serialTransfer.begin(Serial, false);

  while(!Serial);

  camera_config_t config;
  config.ledc_channel = LEDC_CHANNEL_0;
  config.ledc_timer = LEDC_TIMER_0;
  config.pin_d0 = Y2_GPIO_NUM;
  config.pin_d1 = Y3_GPIO_NUM;
  config.pin_d2 = Y4_GPIO_NUM;
  config.pin_d3 = Y5_GPIO_NUM;
  config.pin_d4 = Y6_GPIO_NUM;
  config.pin_d5 = Y7_GPIO_NUM;
  config.pin_d6 = Y8_GPIO_NUM;
  config.pin_d7 = Y9_GPIO_NUM;
  config.pin_xclk = XCLK_GPIO_NUM;
  config.pin_pclk = PCLK_GPIO_NUM;
  config.pin_vsync = VSYNC_GPIO_NUM;
  config.pin_href = HREF_GPIO_NUM;
  config.pin_sscb_sda = SIOD_GPIO_NUM;
  config.pin_sscb_scl = SIOC_GPIO_NUM;
  config.pin_pwdn = PWDN_GPIO_NUM;
  config.pin_reset = RESET_GPIO_NUM;
  config.xclk_freq_hz = 20000000;
  config.frame_size = FRAMESIZE_UXGA;
  config.pixel_format = PIXFORMAT_JPEG; // for streaming
  config.grab_mode = CAMERA_GRAB_WHEN_EMPTY;
  config.fb_location = CAMERA_FB_IN_PSRAM;
  config.jpeg_quality = 12;
  config.fb_count = 1;
  
  // if PSRAM IC present, init with UXGA resolution and higher JPEG quality
  //                      for larger pre-allocated frame buffer.
  if(config.pixel_format == PIXFORMAT_JPEG){
    if(psramFound()){
      config.jpeg_quality = 10;
      config.fb_count = 2;
      config.grab_mode = CAMERA_GRAB_LATEST;
    } else {
      // Limit the frame size when PSRAM is not available
      config.frame_size = FRAMESIZE_SVGA;
      config.fb_location = CAMERA_FB_IN_DRAM;
    }
  }

  esp_err_t err = esp_camera_init(&config);
  if (err != ESP_OK) {
    return;
  }

  sensor_t * s = esp_camera_sensor_get();
  // initial sensors are flipped vertically and colors are a bit saturated
  if (s->id.PID == OV3660_PID) {
    s->set_vflip(s, 1); // flip it back
    s->set_brightness(s, 1); // up the brightness just a bit
    s->set_saturation(s, -2); // lower the saturation
  }
  // drop down frame size for higher initial frame rate
  if(config.pixel_format == PIXFORMAT_JPEG){
    s->set_framesize(s, FRAMESIZE_QVGA);
  }

// Setup LED FLash if LED pin is defined in camera_pins.h
#if defined(LED_GPIO_NUM)
  setupLedFlash(LED_GPIO_NUM);
#endif

  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  WiFi.setSleep(false);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
  }

  startCameraServer();

  // NOTE: Either you'd provide a SSL/TLS cert here, or you call this method to ignore all that.
  //       Since the course does not require us to cover security, as of the time of writing 
  //       we've opted out of implementing that part
  espClient.setInsecure();

  client.setServer(MQTT_BROKER_ADDRESS, MQTT_BROKER_PORT);
  client.setCallback(mqttSubCallback);
}

void publish(MqttTopic topic, uint8_t *ptr, uint8_t size) {
  const char *topicString = getTopicString(topic);
  if (topicString) {
    client.publish(topicString, ptr, size);
  }
}

void mqttSubCallback(String topicString, byte *message, unsigned int length) {
  MqttTopic topic = getTopicByString(topicString);
  serialTransfer.txObj(*message, 0, length);
  serialTransfer.sendData(length, topic);
}

void reconnect() {
  while (!client.connected()) {
    String clientId = MQTT_CLIENT_ID_PREFIX + String(random(0xFFFF));
    if (client.connect(clientId.c_str(), MQTT_USERNAME, MQTT_PASSWORD)) {
      client.subscribe(CAR_CONTROL_TOPIC_STRING);
      client.subscribe(CAMERA_CONTROL_TOPIC_STRING);
    } else {
      delay(5000);
    }
  }
}

void publishCameraAddress() {  
  unsigned long now = millis();
  if(now - lastTimeAddressPublished > CAMERA_ADDRESS_PUBLISH_INTERVAL_MS) {
    lastTimeAddressPublished = now;

    IPAddress address = WiFi.localIP();
    uint8_t message[] {address[0], address[1], address[2], address[3]};
    publish(CAMERA_ADDRESS, message, sizeof(message));
  } 
}

void loop() {
  if (!client.connected()) {
    reconnect();
  }
  
  client.loop();

  uint8_t available = serialTransfer.available();
  if (available) {
    MqttTopic topic = getTopicByByteValue(serialTransfer.currentPacketID());
    publish(topic, serialTransfer.packet.rxBuff, available);
  }

  publishCameraAddress();
}
