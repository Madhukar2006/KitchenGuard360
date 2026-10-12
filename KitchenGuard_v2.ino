/*
  KitchenGuard v2 - ESP32 Dev Module
  Har sensor ki ek limit set hai (neeche SETTINGS me badal sakte ho).
  Buzzer + Red LED sirf FIRE detect hone par chalenge.
  Gas / smoke / temperature limit cross hone par Serial me "HIGH" dikhega.

  Library: DHT sensor library (Adafruit) + Adafruit Unified Sensor
  Serial Monitor: 115200 baud
*/

#include <DHT.h>

// ---------- PINS ----------
#define MQ6_AO     34
#define MQ6_DO     27
#define MQ2_AO     35
#define MQ2_DO     26
#define DHT_PIN    4
#define FLAME_AO   32
#define FLAME_DO   33
#define LED_GREEN  12
#define LED_YELLOW 13
#define LED_RED    14
#define BUZZER     25

#define DHT_TYPE DHT11

// ---------- LIMITS (yahan se badlo) ----------
const int   MQ6_LIMIT   = 1000;  // LPG: isse UPAR = gas high
const int   MQ2_LIMIT   = 1500;  // Smoke: isse UPAR = smoke high
const float TEMP_LIMIT  = 50.0;  // Temperature: isse UPAR = high
const int   FLAME_LIMIT = 100;   // Flame analog: isse NEECHE = fire

// false = sirf flame sensor ka digital pin fire decide kare (safe, recommended)
// true  = analog bhi use ho (FLAME_LIMIT se neeche = fire)
#define USE_ANALOG_FOR_FIRE false

// Flame module DO ka level jab flame ho (zyada tar LOW)
#define FLAME_ACTIVE_LEVEL LOW

// Buzzer LOW par bajta ho to true karo
#define BUZZER_ACTIVE_LOW true

DHT dht(DHT_PIN, DHT_TYPE);

unsigned long lastRead = 0;
const unsigned long READ_INTERVAL = 1000;

float temperature = 0;
float humidity = 0;

void buzzerOn()  { digitalWrite(BUZZER, BUZZER_ACTIVE_LOW ? LOW : HIGH); }
void buzzerOff() { digitalWrite(BUZZER, BUZZER_ACTIVE_LOW ? HIGH : LOW); }

void setup() {
  Serial.begin(115200);

  pinMode(MQ6_DO, INPUT);
  pinMode(MQ2_DO, INPUT);
  pinMode(FLAME_DO, INPUT);

  pinMode(LED_GREEN, OUTPUT);
  pinMode(LED_YELLOW, OUTPUT);
  pinMode(LED_RED, OUTPUT);
  pinMode(BUZZER, OUTPUT);

  digitalWrite(LED_GREEN, LOW);
  digitalWrite(LED_YELLOW, LOW);
  digitalWrite(LED_RED, LOW);
  buzzerOff();

  analogReadResolution(12);  // 0 - 4095
  dht.begin();

  Serial.println("KitchenGuard v2 started...");
}

void loop() {
  int flameAnalog = analogRead(FLAME_AO);
  bool flameDigital = (digitalRead(FLAME_DO) == FLAME_ACTIVE_LEVEL);
  // Jab tak flame hai fire = true, flame hatate hi false (turant stop)
  bool fire = flameDigital;
  if (USE_ANALOG_FOR_FIRE) {
    // true: analog 100 (FLAME_LIMIT) se kam ho to fire, 100 ya upar = stop
    fire = flameDigital || (flameAnalog < FLAME_LIMIT);
  }

  // Fire hone par hi buzzer + red LED
  if (fire) {
    digitalWrite(LED_RED, HIGH);
    buzzerOn();
  } else {
    digitalWrite(LED_RED, LOW);
    buzzerOff();
  }

  if (millis() - lastRead >= READ_INTERVAL) {
    lastRead = millis();

    int mq6 = analogRead(MQ6_AO);
    int mq2 = analogRead(MQ2_AO);

    float t = dht.readTemperature();
    float h = dht.readHumidity();
    if (!isnan(t)) temperature = t;
    if (!isnan(h)) humidity = h;

    Serial.println("----------- SENSOR READINGS -----------");

    Serial.print("MQ-6 (LPG)   : "); Serial.print(mq6);
    Serial.print(" / limit "); Serial.print(MQ6_LIMIT);
    Serial.println(mq6 > MQ6_LIMIT ? "  -> HIGH" : "  -> OK");

    Serial.print("MQ-2 (Smoke) : "); Serial.print(mq2);
    Serial.print(" / limit "); Serial.print(MQ2_LIMIT);
    Serial.println(mq2 > MQ2_LIMIT ? "  -> HIGH" : "  -> OK");

    Serial.print("Temperature  : "); Serial.print(temperature);
    Serial.print(" C / limit "); Serial.print(TEMP_LIMIT);
    Serial.println(temperature > TEMP_LIMIT ? "  -> HIGH" : "  -> OK");

    Serial.print("Humidity     : "); Serial.print(humidity); Serial.println(" %");

    Serial.print("Flame Analog : "); Serial.print(flameAnalog);
    Serial.print(" / limit "); Serial.print(FLAME_LIMIT);
    Serial.print(" | Digital: "); Serial.println(digitalRead(FLAME_DO));

    Serial.print("Status       : ");
    Serial.println(fire ? "FIRE DETECTED! Buzzer + Red LED ON" : "Normal");
    Serial.println();
  }
}
