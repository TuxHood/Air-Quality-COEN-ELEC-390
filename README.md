# Air-Quality-COEN-ELEC-390

An air quality monitoring system that uses an ESP-32 microcontroller connected to air quality sensors (CCS811 and MQ-2) and communicates with an Android application via Bluetooth.

## Project Overview

This project consists of two main components:

1. **Android Application**: A Java-based Android app that connects to the ESP-32 via Bluetooth and displays real-time air quality data.
2. **ESP-32 Firmware**: Arduino code that reads data from air quality sensors and transmits it via Bluetooth Serial.

## Hardware Requirements

### ESP-32 Setup
- ESP-32 Development Board
- CCS811 Air Quality Sensor (CO2 and TVOC)
- MQ-2 Gas Sensor (Smoke detection)
- Jumper wires
- Breadboard (optional)

### Wiring Connections

#### CCS811 Sensor (I2C)
- VCC → 3.3V
- GND → GND
- SDA → GPIO 21
- SCL → GPIO 22

#### MQ-2 Sensor (Analog)
- VCC → 5V (if available) or 3.3V
- GND → GND
- A0 → GPIO 34 (ADC1_CH6)

## Software Requirements

### Android App
- Android Studio Arctic Fox or later
- Minimum Android SDK: 21 (Android 5.0 Lollipop)
- Target Android SDK: 33 (Android 13)
- Gradle 7.4.2 or later

### ESP-32 Firmware
- Arduino IDE 1.8.x or later (or PlatformIO)
- ESP32 Board Support Package
- Required Libraries:
  - Adafruit CCS811 Library
  - BluetoothSerial (built-in with ESP32 board package)

## Installation & Setup

### ESP-32 Firmware Setup

1. **Install Arduino IDE** (if not already installed)
   - Download from [arduino.cc](https://www.arduino.cc/en/software)

2. **Install ESP32 Board Support**
   - Open Arduino IDE
   - Go to File → Preferences
   - Add to "Additional Board Manager URLs": 
     ```
     https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json
     ```
   - Go to Tools → Board → Boards Manager
   - Search for "esp32" and install "ESP32 by Espressif Systems"

3. **Install Required Libraries**
   - Go to Sketch → Include Library → Manage Libraries
   - Search and install "Adafruit CCS811"

4. **Upload Firmware**
   - Open `ESP32_Firmware/ESP32_Firmware.ino` in Arduino IDE
   - Select your ESP32 board: Tools → Board → ESP32 Arduino → ESP32 Dev Module
   - Select the correct COM port: Tools → Port
   - Click Upload button
   - Wait for upload to complete

5. **Verify Operation**
   - Open Serial Monitor (Tools → Serial Monitor)
   - Set baud rate to 115200
   - You should see sensor readings and Bluetooth status

### Android App Setup

1. **Open Project in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the `AndroidApp` folder
   - Click OK

2. **Build the Project**
   - Let Gradle sync complete
   - Build → Make Project (or Ctrl+F9)

3. **Install on Android Device**
   - Connect your Android device via USB
   - Enable Developer Options and USB Debugging on your device
   - Click Run → Run 'app' (or Shift+F10)
   - Select your device and click OK

## Usage

### Pairing ESP-32 with Android

1. **Power on the ESP-32**
   - The device will appear as "ESP32_AirQuality" in Bluetooth settings

2. **Pair the Device**
   - On your Android device, go to Settings → Bluetooth
   - Scan for devices
   - Select "ESP32_AirQuality"
   - Pair with the device (default PIN is usually 1234 or 0000)

3. **Connect via App**
   - Open the Air Quality Monitor app
   - Grant necessary permissions (Bluetooth, Location)
   - Tap "Connect" button
   - Wait for connection to establish

4. **View Air Quality Data**
   - Once connected, you'll see real-time readings:
     - CO2 levels (ppm)
     - TVOC levels (ppb)
     - Smoke detection values
     - Overall air quality status (Good/Moderate/Poor)

### Understanding the Readings

- **CO2 (Carbon Dioxide)**
  - < 1000 ppm: Good air quality
  - 1000-2000 ppm: Moderate - ventilation recommended
  - > 2000 ppm: Poor - immediate ventilation needed

- **TVOC (Total Volatile Organic Compounds)**
  - Indicates presence of various organic chemicals
  - Lower values indicate better air quality

- **Smoke**
  - Raw analog value from MQ-2 sensor
  - Higher values indicate more smoke/gas detected

## Project Structure

```
Air-Quality-COEN-ELEC-390/
├── AndroidApp/                    # Android application
│   ├── app/
│   │   ├── src/
│   │   │   └── main/
│   │   │       ├── java/
│   │   │       │   └── com/airquality/monitor/
│   │   │       │       └── MainActivity.java
│   │   │       ├── res/
│   │   │       │   ├── layout/
│   │   │       │   │   └── activity_main.xml
│   │   │       │   └── values/
│   │   │       │       ├── strings.xml
│   │   │       │       └── themes.xml
│   │   │       └── AndroidManifest.xml
│   │   └── build.gradle
│   ├── build.gradle
│   └── settings.gradle
├── ESP32_Firmware/                # ESP32 Arduino sketch
│   └── ESP32_Firmware.ino
└── README.md                      # This file
```

## Features

### Android App
- Bluetooth Classic connection to ESP-32
- Real-time sensor data display
- User-friendly interface with Material Design
- Connection status indicator
- Air quality assessment based on CO2 levels
- Permission handling for Bluetooth and Location

### ESP-32 Firmware
- Reads CCS811 sensor (CO2 and TVOC)
- Reads MQ-2 sensor (analog smoke/gas detection)
- Bluetooth Serial communication
- Configurable sampling interval (default: 2 seconds)
- Serial debugging output

## Troubleshooting

### ESP-32 Issues

**Problem**: CCS811 sensor not detected
- Check I2C wiring (SDA, SCL)
- Verify sensor power (3.3V)
- Try different I2C address if applicable

**Problem**: Bluetooth not working
- Ensure CONFIG_BT_ENABLED is set in ESP32 configuration
- Verify ESP32 board package is correctly installed
- Check device name in Serial Monitor output

### Android App Issues

**Problem**: Can't find ESP32 device
- Ensure ESP32 is powered on
- Check if device is already paired in Bluetooth settings
- Try unpairing and re-pairing the device

**Problem**: Connection fails
- Verify Bluetooth permissions are granted
- Enable Location services (required for Bluetooth scanning on Android)
- Check if another app is using the Bluetooth connection

**Problem**: No data received
- Verify ESP32 is transmitting (check Serial Monitor)
- Ensure sensors are properly connected
- Check Bluetooth connection status

## License

This project is open source and available for educational purposes.

## Contributors

Developed for COEN-ELEC 390 course project.

## Acknowledgments

- Adafruit for the CCS811 sensor library
- Espressif Systems for ESP32 Arduino core
- Android Open Source Project for Android framework