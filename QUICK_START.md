# Quick Start Guide

## Prerequisites Setup

### For ESP32 Development
1. Download and install Arduino IDE from https://www.arduino.cc/en/software
2. Install ESP32 board support:
   - Open Arduino IDE
   - File → Preferences
   - Add to "Additional Board Manager URLs":
     ```
     https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json
     ```
   - Tools → Board → Boards Manager
   - Search "esp32" and install "ESP32 by Espressif Systems"

3. Install required libraries:
   - Sketch → Include Library → Manage Libraries
   - Search and install: "Adafruit CCS811"

### For Android Development
1. Download and install Android Studio from https://developer.android.com/studio
2. Install Android SDK (will be prompted during first run)
3. Enable USB debugging on your Android device:
   - Settings → About Phone
   - Tap "Build Number" 7 times to enable Developer Options
   - Settings → Developer Options → Enable "USB Debugging"

## Step-by-Step Build Process

### Building ESP32 Firmware

1. **Open the firmware**
   ```
   Open ESP32_Firmware/ESP32_Firmware.ino in Arduino IDE
   ```

2. **Configure board**
   - Tools → Board → ESP32 Arduino → "ESP32 Dev Module"
   - Tools → Upload Speed → "921600"
   - Tools → CPU Frequency → "240MHz (WiFi/BT)"
   - Tools → Flash Size → "4MB (32Mb)"
   - Tools → Partition Scheme → "Default 4MB with spiffs"

3. **Select port**
   - Connect ESP32 via USB
   - Tools → Port → Select your ESP32's COM port (e.g., COM3, /dev/ttyUSB0)

4. **Upload**
   - Click "Upload" button (right arrow icon)
   - Wait for "Done uploading" message

5. **Verify**
   - Tools → Serial Monitor
   - Set baud rate to 115200
   - You should see initialization messages and sensor readings

### Building Android App

1. **Open project**
   ```
   Open Android Studio
   File → Open → Select AndroidApp folder
   ```

2. **Wait for Gradle sync**
   - First build may take several minutes
   - Android Studio will download required dependencies

3. **Build APK**
   - Build → Make Project (Ctrl+F9 / Cmd+F9)
   - Wait for build to complete
   - Check "Build" tab for any errors

4. **Install on device**
   - Connect Android device via USB
   - Click "Run" button (green triangle)
   - Select your device from the list
   - App will be installed and launched

## Testing the System

### 1. Hardware Test
- Verify all sensor connections
- Check power LED on ESP32
- Ensure sensors are properly seated on breadboard

### 2. ESP32 Test
- Open Serial Monitor
- Check for these messages:
  ```
  ESP32 Air Quality Monitor Starting...
  CCS811 sensor initialized successfully!
  Bluetooth initialized. Device name: ESP32_AirQuality
  ```
- Verify sensor readings appear every 2 seconds

### 3. Bluetooth Pairing
- On Android device: Settings → Bluetooth
- Look for "ESP32_AirQuality"
- Tap to pair (PIN: 1234 or 0000 if prompted)

### 4. App Connection
- Open Air Quality Monitor app
- Grant Bluetooth and Location permissions
- Tap "Connect" button
- Status should change to "Connected to ESP32"
- Sensor readings should appear and update

## Troubleshooting Quick Fixes

### ESP32 Won't Upload
- Press and hold "BOOT" button on ESP32 during upload
- Try different USB cable
- Lower upload speed in Tools menu

### Sensor Not Found
- Check I2C connections (SDA, SCL)
- Verify 3.3V power to sensor
- Try adding pull-up resistors (4.7kΩ) to SDA and SCL

### Bluetooth Won't Connect
- Restart ESP32
- Forget and re-pair device in Bluetooth settings
- Check if another app is using the connection

### App Won't Build
- File → Invalidate Caches / Restart
- Delete .gradle folder and rebuild
- Update Android Studio to latest version

### No Sensor Readings
- Check MQ-2 analog connection to GPIO 34
- Verify CCS811 has completed warm-up (few minutes)
- Check Serial Monitor for error messages

## Next Steps

After successful setup:
1. Let sensors warm up for accurate readings (CCS811: 20 minutes, MQ-2: 24 hours)
2. Calibrate MQ-2 sensor in clean air
3. Test in different environments
4. Monitor readings over time
5. Consider adding data logging features

For more details, see README.md
