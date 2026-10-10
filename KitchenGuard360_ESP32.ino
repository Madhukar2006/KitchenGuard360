/*
 * KitchenGuard360 - Official ESP32 Firmware
 * Hardware: ESP32 NodeMCU + MQ-6 (LPG) + MQ-2 (Smoke) + DHT11 (Temp/Hum) + Flame Sensor + LEDs + Buzzer
 * Supports:
 *   1. Wi-Fi Access Point & HTTP WebServer (GET /data)
 *   2. Bluetooth Serial (SPP) Telemetry Streaming & Actuator Commands
 */

#include <WiFi.h>
#include <WebServer.h>
#include <BluetoothSerial.h>
#include <DHT.h>

// Pin Definitions
#define MQ6_PIN 34       // MQ-6 LPG Gas Sensor (Analog A0)
#define MQ2_PIN 35       // MQ-2 Smoke Sensor (Analog A1)
#define FLAME_PIN 32     // Infrared Flame Sensor (Analog / Digital)
#define DHT_PIN 23       // DHT11 Temp & Humidity
#define DHTTYPE DHT11

#define GREEN_LED_PIN 18 // Green LED
#define YELLOW_LED_PIN 19// Yellow LED
#define RED_LED_PIN 21   // Red LED
#define BUZZER_PIN 22    // Active 5V Buzzer

// Network Credentials
const char* ssid = "KitchenGuard_AP";
const char* password = "password123";

WebServer server(80);
BluetoothSerial SerialBT;
DHT dht(DHT_PIN, DHTTYPE);

// Safety Thresholds
const int LPG_WARN = 300;
const int LPG_DANGER = 600;
const int SMOKE_WARN = 200;
const int SMOKE_DANGER = 450;
const float TEMP_WARN = 40.5;
const float TEMP_DANGER = 50.5;

void setup() {
  Serial.begin(115200);

  pinMode(GREEN_LED_PIN, OUTPUT);
  pinMode(YELLOW_LED_PIN, OUTPUT);
  pinMode(RED_LED_PIN, OUTPUT);
  pinMode(BUZZER_PIN, OUTPUT);
  pinMode(FLAME_PIN, INPUT);

  dht.begin();

  // Initialize Bluetooth SPP
  SerialBT.begin("KitchenGuard360_ESP32");
  Serial.println("Bluetooth Serial Active: KitchenGuard360_ESP32");

  // Initialize Wi-Fi Access Point or STA
  WiFi.softAP(ssid, password);
  IPAddress myIP = WiFi.softAPIP();
  Serial.print("Wi-Fi Access Point Active. IP Address: ");
  Serial.println(myIP);

  // Define HTTP Endpoints
  server.on("/", HTTP_GET, handleRoot);
  server.on("/data", HTTP_GET, handleData);
  server.on("/api/sensors", HTTP_GET, handleData);
  server.on("/api/actuators", HTTP_POST, handleActuators);

  server.begin();
  Serial.println("HTTP Web Server Started");
}

void loop() {
  server.handleClient();

  // Handle incoming Bluetooth command frames if available
  if (SerialBT.available()) {
    String btInput = SerialBT.readStringUntil('\n');
    btInput.trim();
    if (btInput.length() > 0) {
      Serial.print("Received BT: ");
      Serial.println(btInput);
    }
  }

  // Stream telemetry over Bluetooth every 2 seconds
  static unsigned long lastBtBroadcast = 0;
  if (millis() - lastBtBroadcast > 2000) {
    lastBtBroadcast = millis();
    String jsonTelemetry = buildSensorJson();
    SerialBT.println(jsonTelemetry);
  }
}

String buildSensorJson() {
  int lpg = analogRead(MQ6_PIN);
  int smoke = analogRead(MQ2_PIN);
  int flame = analogRead(FLAME_PIN);
  float temp = dht.readTemperature();
  float hum = dht.readHumidity();

  if (isnan(temp)) temp = 25.0;
  if (isnan(hum)) hum = 45.0;

  bool isFlamePresent = (flame > 2000);
  bool isDanger = isFlamePresent || (lpg >= LPG_DANGER) || (smoke >= SMOKE_DANGER) || (temp >= TEMP_DANGER);
  bool isWarning = !isDanger && ((lpg >= LPG_WARN) || (smoke >= SMOKE_WARN) || (temp >= TEMP_WARN));

  String status = "SAFE";
  if (isDanger) status = "DANGER";
  else if (isWarning) status = "WARNING";

  // Hardware LED & Buzzer Response
  digitalWrite(GREEN_LED_PIN, status == "SAFE" ? HIGH : LOW);
  digitalWrite(YELLOW_LED_PIN, status == "WARNING" ? HIGH : LOW);
  digitalWrite(RED_LED_PIN, status == "DANGER" ? HIGH : LOW);
  digitalWrite(BUZZER_PIN, status == "DANGER" ? HIGH : LOW);

  String json = "{";
  json += "\"lpg\":" + String(lpg) + ",";
  json += "\"smoke\":" + String(smoke) + ",";
  json += "\"flame\":" + String(flame) + ",";
  json += "\"temperature\":" + String(temp, 1) + ",";
  json += "\"humidity\":" + String(hum, 1) + ",";
  json += "\"alert\":" + String(isDanger || isWarning ? "true" : "false") + ",";
  json += "\"status\":\"" + status + "\"";
  json += "}";

  return json;
}

void handleRoot() {
  server.send(200, "text/plain", "KitchenGuard360 ESP32 Web Server Active");
}

void handleData() {
  String json = buildSensorJson();
  server.send(200, "application/json", json);
}

void handleActuators() {
  if (server.hasArg("plain")) {
    String body = server.arg("plain");
    Serial.println("Actuator POST Body: " + body);
  }
  server.send(200, "application/json", "{\"status\":\"ok\"}");
}
