#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include <NTPClient.h>
#include <WiFiUdp.h>
#include "DHT.h"

#define LDRPIN A0
#define DHTPIN 4        // Chân D2 
#define DHTTYPE DHT11

#define LED_1 14        // Chân D5
#define LED_2 12        // Chân D6 
#define LED_3 13        // Chân D7 

// --- CẤU HÌNH WIFI & MQTT BROKER ---
const char *wifiSsid = "P202_Vua Tây Bắc";      
const char *wifiPassword = "vuahoabinh28";   

const char *mqttServer = "192.168.0.103";    
const int mqttPort = 1865;                  
const char *mqttUsername = "toannd135";     
const char *mqttPassword = "b23dccn831";     
const char *mqttClientID = "ESP8266_Client_01";

const char *mqttSensorDataTopic = "/esp8266/sensors-data";
const char *mqttDeviceControl   = "/esp8266/device-control";
const char *mqttDeviceResponse  = "/esp8266/device-response";

WiFiClient espClient;
PubSubClient mqttClient(espClient);
DHT dht(DHTPIN, DHTTYPE);

WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP, "pool.ntp.org", 25200, 60000); 

unsigned long lastMsg = 0;

String getFormattedTime() {
  time_t epochTime = timeClient.getEpochTime();
  struct tm *ptm = localtime((time_t *)&epochTime);

  char timeBuffer[25];
  sprintf(timeBuffer, "%04d-%02d-%02d %02d:%02d:%02d",
          ptm->tm_year + 1900,
          ptm->tm_mon + 1,
          ptm->tm_mday,
          ptm->tm_hour,
          ptm->tm_min,
          ptm->tm_sec);
  return String(timeBuffer);
}

String getISOTime() {
  time_t epochTime = timeClient.getEpochTime();
  struct tm *ptm = localtime((time_t *)&epochTime);

  char timeBuffer[25];
  sprintf(timeBuffer, "%04d-%02d-%02dT%02d:%02d:%02d",
          ptm->tm_year + 1900,
          ptm->tm_mon + 1,
          ptm->tm_mday,
          ptm->tm_hour,
          ptm->tm_min,
          ptm->tm_sec);
  return String(timeBuffer);
}

void sendDeviceResponse(int deviceId, int userId, const char* action, const char* status) {
  StaticJsonDocument<200> doc;
  doc["device_id"] = deviceId;
  doc["user_id"] = userId;
  doc["action"] = action;
  doc["status"] = status;

  char responseBuffer[200];
  serializeJson(doc, responseBuffer);
  mqttClient.publish(mqttDeviceResponse, responseBuffer);
  Serial.print("[MQTT Out] Phan hoi: ");
  Serial.println(responseBuffer);
}

void callback(char* topic, byte* payload, unsigned int length) {
  StaticJsonDocument<256> doc;
  DeserializationError error = deserializeJson(doc, payload, length);
  if (error) {
    Serial.print("Loi giai ma JSON: ");
    Serial.println(error.f_str());
    return;
  }
  int deviceId = doc["device_id"];
  const char* action = doc["action"];
  int userId = doc["user_id"];

  Serial.print("[MQTT In] Device: ");
  Serial.print(deviceId);
  Serial.print(" | Action: ");
  Serial.print(action);
  Serial.print(" | User: ");
  Serial.println(userId);

   if (strcmp(action, "ALL_ON") == 0) {
    digitalWrite(LED_1, HIGH);
    digitalWrite(LED_2, HIGH);
    digitalWrite(LED_3, HIGH);

    sendDeviceResponse(0, userId, action, "SUCCESS");

    Serial.println("[DEVICE] Da BAT tat ca den");
    return;
  }

  if (strcmp(action, "ALL_OFF") == 0) {
    digitalWrite(LED_1, LOW);
    digitalWrite(LED_2, LOW);
    digitalWrite(LED_3, LOW);

    sendDeviceResponse(0, userId, action, "SUCCESS");

    Serial.println("[DEVICE] Da TAT tat ca den");
    return;
  }

  int targetPin = -1;
  if (deviceId == 1) targetPin = LED_1;
  else if (deviceId == 2) targetPin = LED_2;
  else if (deviceId == 3) targetPin = LED_3;

  if (targetPin != -1) {
    if (strcmp(action, "ON") == 0) {
      digitalWrite(targetPin, HIGH);
      sendDeviceResponse(deviceId, userId, action, "SUCCESS");
    } else if (strcmp(action, "OFF") == 0) {
      digitalWrite(targetPin, LOW);
      sendDeviceResponse(deviceId, userId, action, "SUCCESS");
    } else {
      sendDeviceResponse(deviceId, userId, action, "FAILED");
    }
  } else {
    sendDeviceResponse(deviceId, userId, action, "FAILED");
  }
}

void reconnect() {
  while (!mqttClient.connected()) {
    Serial.print("Dang ket noi MQTT Broker...");
    if (mqttClient.connect(mqttClientID, mqttUsername, mqttPassword)) {
      Serial.println("Thanh cong!");
      mqttClient.subscribe(mqttDeviceControl);
    } else {
      Serial.print("That bai, rc=");
      Serial.print(mqttClient.state());
      Serial.println(" Thu lai sau 5 giay...");
      delay(5000);
    }
  }
}


void setup() {
  Serial.begin(115200);

  pinMode(LED_1, OUTPUT);
  pinMode(LED_2, OUTPUT);
  pinMode(LED_3, OUTPUT);

  digitalWrite(LED_1, LOW);
  digitalWrite(LED_2, LOW);
  digitalWrite(LED_3, LOW);

  WiFi.begin(wifiSsid, wifiPassword);
  Serial.print("Connecting to WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nWiFi connected");

  timeClient.begin();
  timeClient.update();

  mqttClient.setServer(mqttServer, mqttPort);
  mqttClient.setCallback(callback);

  dht.begin();
}

void loop() {
  if (!mqttClient.connected()) {
    reconnect();
  }
  mqttClient.loop();
  timeClient.update();

  unsigned long now = millis();
  if (now - lastMsg > 3000) { 
    lastMsg = now;

    float h = dht.readHumidity();
    float t = dht.readTemperature();
    int ldrValue = analogRead(LDRPIN);

    if (isnan(h) || isnan(t)) {
      Serial.println("Loi doc cam bien DHT!");
      return;
    }

    StaticJsonDocument<384> doc;

    JsonArray data = doc.createNestedArray("data");
    JsonObject sensorItem = data.createNestedObject();
    
    sensorItem["temperature"] = serialized(String(t, 1));
    sensorItem["humidity"] = serialized(String(h, 1));
    sensorItem["light"] = ldrValue;
    sensorItem["timestamp"] = getISOTime();

    char buffer[384];
    serializeJson(doc, buffer);

    Serial.print("[MQTT Out] Payload: ");
    Serial.println(buffer);
    mqttClient.publish(mqttSensorDataTopic, buffer);
  }
}