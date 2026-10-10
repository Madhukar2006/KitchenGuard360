# 🍳 KitchenGuard360

### Smart Kitchen Safety Monitoring System | Android + ESP32 + IoT

KitchenGuard360 is an IoT-based kitchen safety project designed to monitor potential LPG gas leaks, smoke, abnormal temperature, and fire-related hazards using an ESP32 microcontroller, sensors, and an Android application.

The project combines hardware-based environmental monitoring with a mobile interface to help users monitor kitchen conditions and identify potential safety risks.

> **Project:** KitchenGuard360  
> **Category:** Internet of Things (IoT), Embedded Systems, Android Development  
> **Platform:** Android 10+ (target compatibility should be verified against the app configuration)  
> **Microcontroller:** ESP32  
> **Communication:** Bluetooth and Wi-Fi  
> **Language:** Kotlin, Arduino C/C++

---

## ✨ Key Features

- 🌡️ **Temperature Monitoring:** Monitor temperature readings using a compatible temperature sensor.
- 🔥 **Fire Hazard Detection:** Integrate a flame sensor to identify potential fire hazards.
- 💨 **LPG Gas Monitoring:** Use an MQ-6 gas sensor to detect changes associated with LPG.
- 🌫️ **Smoke Monitoring:** Integrate an MQ-2 sensor for smoke and combustible-gas-related detection.
- 📱 **Android Dashboard:** View kitchen safety information through a dedicated mobile application.
- 📡 **Bluetooth Connectivity:** Support communication between the Android application and compatible ESP32 firmware.
- 📶 **Wi-Fi Connectivity:** Provide a Wi-Fi connection mode for supported network-based communication.
- 🚨 **Safety Alerts:** Present hazard-related alerts through the application's alert interface.
- 📊 **Sensor Details:** Access individual sensor information through the sensor screens.
- 🕘 **History Screen:** Provide a dedicated interface for reviewing sensor history.
- ⚙️ **Configurable Settings:** Access application settings and safety-related configuration options.

*Actual sensor readings, notifications, connectivity modes, and history behavior depend on the firmware, hardware wiring, and implemented application logic.*

## 🛠️ Hardware Requirements

| Component | Purpose |
|---|---|
| ESP32 DevKit | Main microcontroller and wireless communication |
| MQ-6 LPG Sensor | LPG-related gas detection |
| MQ-2 Sensor | Smoke and combustible-gas detection |
| Flame Sensor | Flame detection |
| DHT11 Temperature Sensor | Temperature and humidity measurements |
| Buzzer | Audible warning |
| Red, Yellow and Green LEDs | Visual status indication |
| Breadboard | Circuit prototyping |
| Jumper Wires | Component connections |
| USB Cable | Programming and power |
| 5V Power Supply | Power for the prototype |

**Note:** Confirm the voltage and current requirements of each component before connecting it. ESP32 GPIO pins use 3.3V logic and must not receive an unprotected 5V signal.

## 💻 Technology Stack

### Android Application

- Kotlin
- Android SDK
- Jetpack Compose
- Gradle with Kotlin DSL
- Android Bluetooth APIs
- Wi-Fi connectivity support
- ViewModel and application data models

### Hardware and Firmware

- ESP32
- Arduino framework
- Embedded C/C++
- MQ-series gas and smoke sensors
- Temperature and flame sensing
- Digital outputs for indicators and alarms

## 📂 Project Structure

```text
KitchenGuard360/
├── app/
│   ├── src/
│   │   ├── androidTest/
│   │   ├── main/
│   │   │   ├── java/com/kitchenguard360/app/
│   │   │   │   ├── bluetooth/
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/
│   │   │   │   │   ├── preferences/
│   │   │   │   │   └── repository/
│   │   │   │   ├── notifications/
│   │   │   │   ├── ui/
│   │   │   │   │   ├── components/
│   │   │   │   │   ├── navigation/
│   │   │   │   │   ├── screens/
│   │   │   │   │   └── theme/
│   │   │   │   └── viewmodel/
│   │   │   └── res/
│   │   └── test/
│   └── build.gradle.kts
├── gradle/
├── KitchenGuard360_ESP32.ino
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── .gitignore
├── LICENSE
└── README.md
```

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/Madhukar2006/KitchenGuard360.git
```

### 2. Open the Android Project

1. Install Android Studio.
2. Open Android Studio and select **Open**.
3. Choose the cloned `KitchenGuard360` directory.
4. Allow Gradle to sync the project.
5. Resolve any SDK or dependency prompts if they appear.

### 3. Build the Android App

From the project directory, run the following command on Windows:

```powershell
.\gradlew.bat assembleDebug
```

The generated debug APK is normally located at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Alternatively, select **Build > Build APK(s)** in Android Studio.

### 4. Upload the ESP32 Firmware

1. Install the Arduino IDE.
2. Install ESP32 board support.
3. Open `KitchenGuard360_ESP32.ino`.
4. Select the appropriate ESP32 board and COM port.
5. Verify the sensor pin assignments against your actual wiring.
6. Compile and upload the firmware.
7. Open Serial Monitor to inspect the firmware output.

Install any required libraries if the firmware specifies them.

### 5. Connect the Android App

1. Power on the ESP32.
2. Grant the required Bluetooth or nearby-device permissions on the phone.
3. Enable Bluetooth or connect to the configured Wi-Fi network, depending on the selected connection mode.
4. Open the connectivity screen in KitchenGuard360.
5. Select the supported connection method and connect to the ESP32.
6. Confirm that incoming readings match the sensor output.

**Important:** Bluetooth and Wi-Fi are separate communication methods. Each requires matching firmware and Android implementation. Verify the actual connection protocol before testing.

## 🔌 System Architecture

```text
MQ-6 Gas Sensor ─────┐
MQ-2 Smoke Sensor ───┤
Flame Sensor ────────┤
DHT11 Sensor ────────┤
                     ▼
                   ESP32
                     │
          ┌──────────┴──────────┐
          │                     │
       Bluetooth              Wi-Fi
          │                     │
          └──────────┬──────────┘
                     ▼
             KitchenGuard360
               Android App
                     │
          ┌──────────┼──────────┐
          ▼          ▼          ▼
       Dashboard   Alerts     History
```

*Conceptual architecture: the actual data path depends on the selected connection mode and firmware implementation.*

## 🧪 Testing Checklist

- [ ] ESP32 powers on correctly.
- [ ] Sensor readings are visible in the Serial Monitor.
- [ ] Gas, smoke, flame, and temperature sensors are individually tested.
- [ ] Buzzer and LED outputs respond to the configured conditions.
- [ ] Android application builds successfully.
- [ ] Bluetooth connection is tested on a physical Android device.
- [ ] Wi-Fi connection is tested if supported by the firmware.
- [ ] Received sensor data is compared against actual sensor readings.
- [ ] Alert behavior is tested under controlled conditions.

## 🔮 Future Improvements

- Cloud-based monitoring and event history.
- Remote notifications and emergency contact alerts.
- Configurable sensor thresholds.
- Sensor calibration and improved false-alarm handling.
- Network-based dashboards and device status monitoring.
- Automated testing and improved connectivity diagnostics.
- Support for additional environmental sensors.

## ⚠️ Safety Disclaimer

KitchenGuard360 is an educational prototype intended for experimentation and demonstration.

Low-cost gas, smoke, flame, and temperature sensors have limitations and may produce false positives or fail to detect hazards. Sensor readings depend on calibration, placement, environmental conditions, and power supply.

**Do not use this prototype as a replacement for certified LPG detectors, fire alarms, or other approved safety equipment.** Never intentionally release LPG or create an open flame to test the system. Follow appropriate laboratory and electrical safety practices.

## 🤝 Contributing

Contributions, bug reports, and suggestions are welcome.

1. Fork this repository.
2. Create a feature branch.
3. Make and test your changes.
4. Submit a pull request with a clear description.

## 📄 License

This project is distributed under the license included in the repository. See [LICENSE](LICENSE) for the applicable terms.

## 👨‍💻 Author

**Madhukar Pal**

- GitHub: [@Madhukar2006](https://github.com/Madhukar2006)
- Project Repository: [KitchenGuard360](https://github.com/Madhukar2006/KitchenGuard360)

---

⭐ If you find this project useful, consider starring the repository!

**Built with curiosity and a focus on safer kitchens.** 🍳
