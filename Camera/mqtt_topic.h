#pragma once

const char *CAR_CONTROL_TOPIC_STRING    = "car/control";
const char *CAR_TEST_TOPIC_STRING       = "car/test";
const char *CAMERA_ADDRESS_TOPIC_STRING = "camera/address";
const char *CAMERA_CONTROL_TOPIC_STRING = "camera/control";
const char *CAR_COLLISION_TOPIC_STRING  = "car/collision";

enum MqttTopic : uint8_t {
  NONE = 0,           // 0 is none due to internals of SerialTransfer lib
  CAR_CONTROL = 1,
  CAMERA_ADDRESS = 2,
  CAMERA_CONTROL = 3,
  CAR_COLLISION = 4,
  CAR_TEST = 254,     // temporary topic for test/demo, remove later
  UNKNOWN = 255       // denotes ids that are not defined in this enum set
};

MqttTopic getTopicByString(const String &topicString) {
  if (topicString == CAR_CONTROL_TOPIC_STRING) {
    return CAR_CONTROL;
  } else if (topicString == CAR_TEST_TOPIC_STRING) {
    return CAR_TEST;
  } else if (topicString == CAMERA_CONTROL_TOPIC_STRING) {
    return CAMERA_CONTROL;
  } else {
    return UNKNOWN;
  }
}

MqttTopic getTopicByByteValue(uint8_t value) {
  switch (value) {
    case 0: return NONE;
    case 1: return CAR_CONTROL;
    case 3: return CAMERA_CONTROL;
    case 4: return CAR_COLLISION;
    case 254: return CAR_TEST;
    default: return UNKNOWN;
  }
}

const char* getTopicString(MqttTopic topic) {
  switch (topic) {
    case CAR_CONTROL: return CAR_CONTROL_TOPIC_STRING;
    case CAR_TEST: return CAR_TEST_TOPIC_STRING;
    case CAMERA_ADDRESS: return CAMERA_ADDRESS_TOPIC_STRING;
    case CAMERA_CONTROL: return CAMERA_CONTROL_TOPIC_STRING;
    case CAR_COLLISION: return CAR_COLLISION_TOPIC_STRING;
    default: return nullptr;
  }
}
