#include "SerialTransfer.h"
#include "Servo.h"
#include "Ultrasonic.h"

#include "mqtt_messages.h"

SerialTransfer serialTransfer;
Servo servo;
Ultrasonic ultrasonic(2);

int distanceThreshold = 25;
int rangerIn = 2;
//Front right wheel wheelSpeed
int FRWS = 11;
//Front right wheel direction
int FRWD1 = A5;
int FRWD2 = A4;

//servo output pin
int servoOutput = 9;

//Front left wheel wheelSpeed
int FLWS = 5;
//Front left wheel direction
int FLWD1 = A1;
int FLWD2 = A0;


//Back left wheel wheelSpeed
int BLWS = 3;
//Back left wheel direction
int BLWD1 = 8;
int BLWD2 = 7;

//Back right wheel wheelSpeed
int BRWS = 6;
//Back right wheel direction
int BRWD1 = A3;
int BRWD2 = A2;

int collisionLED = 4;

String wheels[4] = { "FR", "FL", "BL", "BR" };

bool collided = false;
CarControlMessage lastMessage;
// TODO: move
unsigned long previousMillis = 0UL;
unsigned long pause = 200UL;
int distanceCM = 250;  // TODO: fix

void setup() {
  Serial.begin(115200);
  serialTransfer.begin(Serial, false);

  // wait for the serial connection to initialize
  while (!Serial)
    ;


  pinMode(FRWD1, OUTPUT);
  pinMode(FRWD2, OUTPUT);
  pinMode(FRWS, OUTPUT);

  pinMode(FLWD1, OUTPUT);
  pinMode(FLWD2, OUTPUT);
  pinMode(FLWS, OUTPUT);

  pinMode(BLWD1, OUTPUT);
  pinMode(BLWD2, OUTPUT);
  pinMode(BLWS, OUTPUT);

  pinMode(BRWD1, OUTPUT);
  pinMode(BRWD2, OUTPUT);
  pinMode(BRWS, OUTPUT);

  pinMode(collisionLED, OUTPUT);

  servo.attach(servoOutput);
  servo.write(90);
}

void publish(MqttTopic topic, uint8_t *ptr, uint8_t size) {
  serialTransfer.txObj(*ptr, 0, size);
  serialTransfer.sendData(size, topic);
}

void handleCarControlMessage(const CarControlMessage *message) {
 
  lastMessage = *message;

  if (message->accelerationDirection == FORWARD) {
    if (message->steeringAmount >= 128) {
      for (int i = 0; i < 4; i++) {
        allWheelSpeed(message->accelerationAmount);
      }

      if (!collided) {
        turn(message->steeringDirection);
      }
    } else {
      moveAllWheelsForward(message->accelerationAmount);
    }
  } else if (message->accelerationDirection == REVERSE) {
    moveAllWheelsBackward(message->accelerationAmount);
  } else {
    stopAllWheels();
  }
}

void turn(SteeringDirection steeringDirection){
  if(steeringDirection == LEFT){
    wheelBackwards("FL");
    wheelBackwards("BL");
    wheelForwards("FR");
    wheelForwards("BR");
  }else if(steeringDirection == RIGHT){
    wheelForwards("FL");
    wheelForwards("BL");
    wheelBackwards("FR");
    wheelBackwards("BR");
  }
}

void mqttSubCallback(MqttTopic topic, uint8_t *ptr, uint8_t length) {
  switch (topic) {
    case CAR_CONTROL:
      {
        if (length < sizeof(CarControlMessage)) {
          return;
        }

        CarControlMessage *message = reinterpret_cast<CarControlMessage *>(ptr);
        handleCarControlMessage(message);
      }
      break;
    case CAMERA_CONTROL:
      {
        controlCamera(*ptr);
      }
      break;
    default:
      {
        char buffer[64];
        uint16_t stringLength = sprintf(buffer, "Echo back of topic %d!", topic);
        publish(CAR_TEST, buffer, stringLength);
      }
      break;
  }
}

//DC motor control code
void wheelForwards(String wheelName) {
  // to make sure a mistake can't be made with the input
  if (wheelName.compareTo("FL") == 0) {
    digitalWrite(FLWD1, LOW);
    digitalWrite(FLWD2, HIGH);
  } else if (wheelName.compareTo("FR") == 0) {
    digitalWrite(FRWD1, HIGH);
    digitalWrite(FRWD2, LOW);
  } else if (wheelName.compareTo("BL") == 0) {
    digitalWrite(BLWD1, LOW);
    digitalWrite(BLWD2, HIGH);
  } else if (wheelName.compareTo("BR") == 0) {
    digitalWrite(BRWD1, HIGH);
    digitalWrite(BRWD2, LOW);
  }
}

void wheelBackwards(String wheelName) {
  // to make sure a mistake can't be made with the input
  if (wheelName.compareTo("FL") == 0) {
    digitalWrite(FLWD2, LOW);
    digitalWrite(FLWD1, HIGH);
  } else if (wheelName.compareTo("FR") == 0) {
    digitalWrite(FRWD1, LOW);
    digitalWrite(FRWD2, HIGH);
  } else if (wheelName.compareTo("BL") == 0) {
    digitalWrite(BLWD2, LOW);
    digitalWrite(BLWD1, HIGH);
  } else if (wheelName.compareTo("BR") == 0) {
    digitalWrite(BRWD1, LOW);
    digitalWrite(BRWD2, HIGH);
  }
}

void moveAllWheelsBackward(uint8_t wheelSpeed) {
  for (int i = 0; i < 4; i++) {
    allWheelSpeed(wheelSpeed);
    wheelBackwards(wheels[i]);
  }
}

void setWheelSpeed(String wheelName, uint8_t wheelSpeed) {
  //exceptions missing
  if (wheelName.compareTo("FL") == 0) {
    analogWrite(FLWS, wheelSpeed);
  } else if (wheelName.compareTo("FR") == 0) {
    analogWrite(FRWS, wheelSpeed);
  } else if (wheelName.compareTo("BL") == 0) {
    analogWrite(BLWS, wheelSpeed);
  } else if (wheelName.compareTo("BR") == 0) {
    analogWrite(BRWS, wheelSpeed);
  }
}

void allWheelSpeed(uint8_t wheelSpeed) {
  for (int i = 0; i < 4; i++) {
    setWheelSpeed(wheels[i], wheelSpeed);
  }
}

void moveAllWheelsForward(uint8_t wheelSpeed) {
  if (collided) {
    return;
  }
  //for testing in case motors are faulty
  //digitalWrite(leftLED, LOW);
  //digitalWrite(rightLED, LOW);
  //digitalWrite(straightLED, HIGH);
  for (int i = 0; i < 4; i++) {
    allWheelSpeed(wheelSpeed);
    wheelForwards(wheels[i]);
  }
}

void controlCamera(byte cameraDirection) {
  switch (cameraDirection) {
    case CAMERA_LEFT:
      {
        int servoFuturePosition = servo.read() - 10;
        if (servoFuturePosition >= 0) {
          servo.write(servoFuturePosition);
        }
      }
      break;
    case CAMERA_RIGHT:
      {
        int servoFuturePosition = servo.read() + 10;
        if (servoFuturePosition <= 180) {
          servo.write(servoFuturePosition);
        }
      }
      break;
      //missing default case
  }
}

void stopAllWheels() {
  // 4 is hardcoded :|
  for (int i = 0; i < 4; i++) {
    stopWheel(wheels[i]);
  }
}

void stopWheel(String wheelName) {
  if (wheelName.compareTo("FL") == 0) {
    digitalWrite(FLWD1, LOW);
    digitalWrite(FLWD2, LOW);
  } else if (wheelName.compareTo("FR") == 0) {
    digitalWrite(FRWD1, LOW);
    digitalWrite(FRWD2, LOW);
  } else if (wheelName.compareTo("BL") == 0) {
    digitalWrite(BLWD1, LOW);
    digitalWrite(BLWD2, LOW);
  } else if (wheelName.compareTo("BR") == 0) {
    digitalWrite(BRWD1, LOW);
    digitalWrite(BRWD2, LOW);
  }
}

int returnDistanceCM(int rangerPin) {
  unsigned long currentMillis = millis();
  if (currentMillis - previousMillis >= pause) {
    distanceCM = ultrasonic.MeasureInCentimeters();
    previousMillis = currentMillis;
  }
  return distanceCM;
}

void publishCollision() {
  char buffer[] = "Collision detected!";
  uint16_t stringLength = sizeof(buffer) - 1;

  // Sending the collision message
  publish(CAR_COLLISION, (uint8_t *)buffer, stringLength);
}

void monitorDistance() {
  if (returnDistanceCM(rangerIn) < distanceThreshold) {
    digitalWrite(collisionLED, HIGH);

    // TODO: limit rate of publishing as well?
    publishCollision();

    if (lastMessage.accelerationDirection == FORWARD) {
      stopAllWheels();
    }

    collided = true;
  } else {
    digitalWrite(collisionLED, LOW);
    collided = false;
  }
}

void loop() {
  uint8_t available = serialTransfer.available();
  if (available) {
    MqttTopic topic = getTopicByByteValue(serialTransfer.currentPacketID());
    mqttSubCallback(topic, serialTransfer.packet.rxBuff, available);
  }

  monitorDistance();
}
