#pragma once

enum MqttTopic : uint8_t {
  NONE = 0,           // 0 is none due to internals of SerialTransfer lib
  CAR_CONTROL = 1,
  CAMERA_CONTROL = 3,
  CAR_COLLISION = 4,
  CAR_TEST = 254,     // temporary topic for test/demo, remove later
  UNKNOWN = 255       // denotes ids that are not defined in this enum set
};

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

enum CameraDirection : uint8_t {   
  CAMERA_LEFT = 1,  
  CAMERA_RIGHT = 2  
};
  
struct CarControlMessage {  
  AccelerationDirection accelerationDirection;  
  uint8_t accelerationAmount;  
  SteeringDirection steeringDirection;  
  uint8_t steeringAmount;  
};
