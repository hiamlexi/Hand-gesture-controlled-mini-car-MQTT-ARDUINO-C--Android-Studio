#pragma once

const char *CAR_CONTROL_TOPIC_STRING = "car/control";
const char *CAR_TEST_TOPIC_STRING    = "car/test";

enum MqttTopic : uint8_t {
  NONE = 0,           // 0 is none due to internals of SerialTransfer lib
  CAR_CONTROL = 1,
  CAR_TEST = 254,     // temporary topic for test/demo, remove later
  UNKNOWN = 255       // denotes ids that are not defined in this enum set
};

MqttTopic getTopicByString(const String &topicString) {
  if (topicString == CAR_CONTROL_TOPIC_STRING) {
    return CAR_CONTROL;
  } else if (topicString == CAR_TEST_TOPIC_STRING) {
    return CAR_TEST;
  } else {
    return UNKNOWN;
  }
}

MqttTopic getTopicByByteValue(uint8_t value) {
  switch (value) {
    case 0: return NONE;
    case 1: return CAR_CONTROL;
    case 254: return CAR_TEST;
    default: return UNKNOWN;
  }
}

const char* getTopicString(MqttTopic topic) {
  switch (topic) {
    case CAR_CONTROL: return CAR_CONTROL_TOPIC_STRING;
    case CAR_TEST: return CAR_TEST_TOPIC_STRING;
    default: return nullptr;
  }
}
