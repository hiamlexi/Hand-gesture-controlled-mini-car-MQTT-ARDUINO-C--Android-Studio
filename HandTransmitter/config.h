// Uncomment defines and enter credentials within the quotation marks
// NOTE: Do not commit any changes here. The file is added to .gitignore.

#define WIFI_SSID "meoca's iPhone"
#define WIFI_PASSWORD "123123123"

#define MQTT_BROKER_ADDRESS "f8a667c650f64b02b7100b463f15dacd.s2.eu.hivemq.cloud"
#define MQTT_BROKER_PORT 8883
#define MQTT_USERNAME "Test1234"
#define MQTT_PASSWORD "Test1234"

#if !defined(WIFI_SSID) || !defined(WIFI_PASSWORD)
  #error "Must setup wifi credentials!"
#endif

#if !defined(MQTT_BROKER_ADDRESS) || !defined(MQTT_BROKER_PORT) || !defined(MQTT_USERNAME) || !defined(MQTT_PASSWORD)
  #error "Must define MQTT connection details!"
#endif
