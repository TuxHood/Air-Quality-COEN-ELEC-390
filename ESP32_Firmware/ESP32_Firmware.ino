/*
 * ESP32 Air Quality Monitor
 * 
 * This firmware reads data from CCS811 (CO2 and TVOC) and MQ-2 (Smoke) sensors
 * and transmits the data to an Android app via Bluetooth Serial.
 * 
 * Hardware connections:
 * - CCS811: I2C (SDA=21, SCL=22)
 * - MQ-2: Analog (A0=34)
 * 
 * Required libraries:
 * - Adafruit CCS811 Library
 * - BluetoothSerial (built-in ESP32 library)
 */

#include <Wire.h>
#include <Adafruit_CCS811.h>
#include "BluetoothSerial.h"

// Check if Bluetooth is available
#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to enable it
#endif

// Sensor objects
Adafruit_CCS811 ccs;
BluetoothSerial SerialBT;

// Pin definitions
#define MQ2_PIN 34  // Analog pin for MQ-2 sensor

// Bluetooth device name
const char* deviceName = "ESP32_AirQuality";

// Timing variables
unsigned long lastReadTime = 0;
const unsigned long READ_INTERVAL = 2000;  // Read sensors every 2 seconds

void setup() {
  // Initialize serial communication for debugging
  Serial.begin(115200);
  Serial.println("ESP32 Air Quality Monitor Starting...");

  // Initialize I2C
  Wire.begin(21, 22);  // SDA=21, SCL=22

  // Initialize CCS811 sensor
  if (!ccs.begin()) {
    Serial.println("Failed to start CCS811 sensor! Please check your wiring.");
    while (1) {
      delay(1000);
    }
  }

  // Wait for the CCS811 sensor to be ready
  while (!ccs.available()) {
    delay(100);
  }

  Serial.println("CCS811 sensor initialized successfully!");

  // Initialize Bluetooth
  if (!SerialBT.begin(deviceName)) {
    Serial.println("An error occurred initializing Bluetooth");
    while (1) {
      delay(1000);
    }
  }

  Serial.println("Bluetooth initialized. Device name: " + String(deviceName));
  Serial.println("You can now pair with the device from your Android app");

  // Initialize MQ-2 analog pin
  pinMode(MQ2_PIN, INPUT);

  Serial.println("Setup complete. Starting sensor readings...");
}

void loop() {
  unsigned long currentTime = millis();

  // Read sensors at specified interval
  if (currentTime - lastReadTime >= READ_INTERVAL) {
    lastReadTime = currentTime;

    // Read CCS811 sensor
    if (ccs.available()) {
      if (!ccs.readData()) {
        uint16_t co2 = ccs.geteCO2();
        uint16_t tvoc = ccs.getTVOC();

        // Read MQ-2 sensor (analog value)
        int smokeValue = analogRead(MQ2_PIN);

        // Print to serial for debugging
        Serial.print("CO2: ");
        Serial.print(co2);
        Serial.print(" ppm, TVOC: ");
        Serial.print(tvoc);
        Serial.print(" ppb, Smoke: ");
        Serial.println(smokeValue);

        // Send data via Bluetooth in format: "CO2:value,TVOC:value,SMOKE:value"
        String dataString = "CO2:" + String(co2) + ",TVOC:" + String(tvoc) + ",SMOKE:" + String(smokeValue);
        SerialBT.println(dataString);

      } else {
        Serial.println("Error reading CCS811 sensor data");
      }
    }
  }

  // Small delay to prevent watchdog timer issues
  delay(10);
}
