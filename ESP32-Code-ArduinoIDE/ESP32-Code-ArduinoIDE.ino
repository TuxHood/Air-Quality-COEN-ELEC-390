// NimBLE-based replacement for BluetoothSerial (SPP) on ESP32-S3
#include <NimBLEDevice.h>
#include <Wire.h>
#include <MQ2.h>
#include <Arduino.h>
#include <SparkFunCCS811.h>

#define DEVICE_NAME "AQ-Device"

// UUIDs for Nordic UART Service (NUS)
static BLEUUID serviceUUID("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
static BLEUUID txCharUUID("6E400003-B5A3-F393-E0A9-E50E24DCCA9E"); // notify (device -> client)

NimBLECharacteristic* pTxCharacteristic = nullptr;

// Sensors
const int MQ2_PIN = 34; // Analog input for MQ-2, change if needed
unsigned long lastSensorPublish = 0;
const unsigned long SENSOR_PUBLISH_INTERVAL = 5000; // ms

// (No RPC/JSON handlers needed — this sketch only publishes sensor data.)

#define CCS811_ADDR 0x5A//CCS811 address

CCS811 mySensor(CCS811_ADDR);//creating a CCS811 instance
MQ2 gasTest(MQ2_PIN);//creating a MQ-2 instance 

void setup() {
  Wire.begin(21,22);//CCS811 sensor turn on Wire.begin(SCA,SCL)
  delay(1000);//wait for CCS811 to turn on
  Serial.begin(115200);
  delay(100);

   gasTest.begin();//MQ-2 sensor begins
  delay(1000);//wait for MQ-2 to turn on
  if (mySensor.begin() == false)
  {
    Serial.print("CCS811 error. Please check wiring. Freezing...");
    //while (1);
  }
Serial.println("-----------------------------------------------");
  // BLE init
  NimBLEDevice::init(DEVICE_NAME);
  NimBLEServer* pServer = NimBLEDevice::createServer();
  NimBLEService* pService = pServer->createService(serviceUUID);
  pTxCharacteristic = pService->createCharacteristic(txCharUUID, NIMBLE_PROPERTY::NOTIFY);
  // Ensure the standard CCCD (0x2902) exists so Android clients can enable
  // notifications by writing the descriptor. Create it directly to avoid
  // depending on helper classes that may not be present in this build.
  pTxCharacteristic->createDescriptor(NimBLEUUID((uint16_t)0x2902));

  pService->start();

  NimBLEAdvertising* pAdv = NimBLEDevice::getAdvertising();
  pAdv->addServiceUUID(serviceUUID);
  NimBLEDevice::setDeviceName(DEVICE_NAME);
  pAdv->setName(DEVICE_NAME);
  {
    NimBLEAdvertisementData scanData;
    scanData.setName(DEVICE_NAME, true);
    pAdv->setScanResponseData(scanData);
  }
  pAdv->start();

  Serial.print("BLE NUS server started. Device address: ");
  Serial.println(NimBLEDevice::getAddress().toString().c_str());
  Serial.println("Subscribe to the TX characteristic to receive sensor data.");
}

void loop() {
  unsigned long now = millis();
  if (now - lastSensorPublish >= SENSOR_PUBLISH_INTERVAL) {
    lastSensorPublish = now;

      // Read sensors
      float eCO2 = 0.0f;
      float tvoc = 0.0f;
      // Prefer SparkFun CCS811 instance (`mySensor`) if data available
      if (mySensor.dataAvailable()) {
        // readAlgorithmResults updates internal values used by getCO2/getTVOC
        mySensor.readAlgorithmResults();
        eCO2 = mySensor.getCO2();
        tvoc = mySensor.getTVOC();
      }

      // MQ-2: use library readings from gasTest where possible
      int mq2_adc = analogRead(MQ2_PIN);
      float lpg_ppm = 0.0f;
      float co_ppm = 0.0f;
      float smoke_ppm = 0.0f;
      float alcohol_ppm = 0.0f;
      float methane_ppm = 0.0f;
      float h2_ppm = 0.0f;
      // Attempt to read from MQ2 library instance `gasTest`. If those methods
      // are not available at runtime, they will likely return 0 — keep the
      // ADC value as a fallback for debugging.
      lpg_ppm = gasTest.readLPG();
      co_ppm = gasTest.readCO();
      smoke_ppm = gasTest.readSmoke();
      alcohol_ppm = gasTest.readAlcohol();
      methane_ppm = gasTest.readMethane();
      h2_ppm = gasTest.readH2();

      // Build raw float array (8 floats = 32 bytes) in LITTLE_ENDIAN order
      // Order chosen to match Android client's expectation: co2, tvoc,
      // lpg, co, smoke, alcohol, methane, h2
      if (pTxCharacteristic) {
        float values[8];
        values[0] = eCO2;         // mySensor.getCO2()
        values[1] = tvoc;         // mySensor.getTVOC()
        values[2] = lpg_ppm;      // gasTest.readLPG()
        values[3] = co_ppm;       // gasTest.readCO()
        values[4] = smoke_ppm;    // gasTest.readSmoke()
        values[5] = alcohol_ppm;  // gasTest.readAlcohol()
        values[6] = methane_ppm;  // gasTest.readMethane()
        values[7] = h2_ppm;       // gasTest.readH2()

        // Copy floats to a byte buffer. ESP32 is little-endian, and Android
        // code expects LITTLE_ENDIAN, so a direct memcpy is fine.
        uint8_t outbuf[8 * sizeof(float)];
        memcpy(outbuf, values, sizeof(outbuf));

        // Send raw bytes over BLE
        pTxCharacteristic->setValue(outbuf, sizeof(outbuf));
        pTxCharacteristic->notify();

        // Debug print: show values in readable form
        Serial.print("Published floats: ");
        Serial.print("CO2="); Serial.print(values[0]);
        Serial.print(" TVOC="); Serial.print(values[1]);
        Serial.print(" LPG="); Serial.print(values[2]);
        Serial.print(" CO="); Serial.print(values[3]);
        Serial.print(" SMOKE="); Serial.print(values[4]);
        Serial.print(" ALCOHOL="); Serial.print(values[5]);
        Serial.print(" METH="); Serial.print(values[6]);
        Serial.print(" H2="); Serial.println(values[7]);
      }
  }

  delay(20);
}