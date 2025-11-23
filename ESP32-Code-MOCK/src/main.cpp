// NimBLE-based replacement for BluetoothSerial (SPP) on ESP32-S3
#include <NimBLEDevice.h>
#include <Wire.h>
#include <MQ2.h>
#include <Arduino.h>
#include <math.h>
#include <SparkFunCCS811.h>

// Set to 1 to run without actual sensors (mock data). When enabled we
// skip I2C/ADC initialization and avoid calls that require hardware.
#define USE_MOCK_SENSORS 1

#define DEVICE_NAME "AQ-Device"

// UUIDs for Nordic UART Service (NUS)
static BLEUUID serviceUUID("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
static BLEUUID txCharUUID("6E400003-B5A3-F393-E0A9-E50E24DCCA9E"); // notify (device -> client)

NimBLECharacteristic* pTxCharacteristic = nullptr;
NimBLEServer* pServer = nullptr;
static volatile bool clientConnected = false;
NimBLEAdvertising* pAdv = nullptr;

// Advertising watchdog settings
const unsigned long ADV_WATCH_INTERVAL = 5000; // ms
const int ADV_RESTART_MAX = 6;
static int advRestartAttempts = 0;

class ServerCallbacks: public NimBLEServerCallbacks {
  void onConnect(NimBLEServer* s) {
    clientConnected = true;
    Serial.println("BLE client connected");
  }
  void onDisconnect(NimBLEServer* s) {
    clientConnected = false;
    Serial.println("BLE client disconnected");
    // Keep advertising so clients can reconnect
    if (pAdv) pAdv->start();
  }
};

// Sensors
const int MQ2_PIN = 34; // Analog input for MQ-2, change if needed
unsigned long lastSensorPublish = 0;
const unsigned long SENSOR_PUBLISH_INTERVAL = 5000; // ms

// (No RPC/JSON handlers needed — this sketch only publishes sensor data.)

#define CCS811_ADDR 0x5A//CCS811 address

CCS811 mySensor(CCS811_ADDR);//creating a CCS811 instance
MQ2 gasTest(MQ2_PIN);//creating a MQ-2 instance 

void setup() {
  Serial.begin(115200);
  delay(100);
  // Initialize hardware only when not using mocks
#if !USE_MOCK_SENSORS
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
#else
  // Mock mode: start Serial early so mock data appears in terminal.
  Serial.begin(115200);
  delay(100);
#endif
Serial.println("-----------------------------------------------");
  // BLE init
  NimBLEDevice::init(DEVICE_NAME);
  pServer = NimBLEDevice::createServer();
  pServer->setCallbacks(new ServerCallbacks());
  NimBLEService* pService = pServer->createService(serviceUUID);
  pTxCharacteristic = pService->createCharacteristic(txCharUUID, NIMBLE_PROPERTY::NOTIFY);
  // Ensure the standard CCCD (0x2902) exists so Android clients can enable
  // notifications by writing the descriptor. Create it directly to avoid
  // depending on helper classes that may not be present in this build.
  pTxCharacteristic->createDescriptor(NimBLEUUID((uint16_t)0x2902));

  pService->start();

  pAdv = NimBLEDevice::getAdvertising();
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

      // Read sensors (only when not mocking)
      float eCO2 = 0.0f;
      float tvoc = 0.0f;
      float lpg_ppm = 0.0f;
      float co_ppm = 0.0f;
      float smoke_ppm = 0.0f;
      float alcohol_ppm = 0.0f;
      float methane_ppm = 0.0f;
      float h2_ppm = 0.0f;
#if !USE_MOCK_SENSORS
      if (mySensor.dataAvailable()) {
        mySensor.readAlgorithmResults();
        eCO2 = mySensor.getCO2();
        tvoc = mySensor.getTVOC();
      }
      int mq2_adc = analogRead(MQ2_PIN);
      lpg_ppm = gasTest.readLPG();
      co_ppm = gasTest.readCO();
      smoke_ppm = gasTest.readSmoke();
      alcohol_ppm = gasTest.readAlcohol();
      methane_ppm = gasTest.readMethane();
      h2_ppm = gasTest.readH2();
#endif

      // Build raw float array (8 floats = 32 bytes) in LITTLE_ENDIAN order
      // Order chosen to match Android client's expectation: co2, tvoc,
      // lpg, co, smoke, alcohol, methane, h2
      if (pTxCharacteristic && pServer) {
            // Generate mock data when hardware isn't present. This keeps the
            // same ordering expected by the Android client: co2, tvoc,
            // lpg, co, smoke, alcohol, methane, h2.
            // We'll synthesize smooth, time-varying signals with small jitter.
            float values[8];
            {
              float t = now / 1000.0f; // seconds
              static bool seeded = false;
              if (!seeded) {
                // Seed pseudo-random generator. In mock mode avoid ADC reads
                // because some boards (or this environment) may not have ADC
                // on the chosen pin; use millis() for variability instead.
    #if USE_MOCK_SENSORS
                randomSeed((unsigned long)millis());
    #else
                randomSeed((unsigned long)millis() ^ (unsigned long)analogRead(MQ2_PIN));
    #endif
                seeded = true;
              }

              auto jitter = [](float amp) -> float {
                long r = random(-1000, 1001);
                return ((float)r / 1000.0f) * amp;
              };

              // Base / amplitude values (tweakable)
              const float co2_base = 400.0f, co2_amp = 50.0f;
              const float tvoc_base = 0.5f, tvoc_amp = 0.5f;
              const float lpg_base = 20.0f, lpg_amp = 30.0f;
              const float co_base = 0.5f, co_amp = 2.0f;
              const float smoke_base = 5.0f, smoke_amp = 10.0f;
              const float alcohol_base = 0.2f, alcohol_amp = 1.0f;
              const float methane_base = 50.0f, methane_amp = 60.0f;
              const float h2_base = 1.0f, h2_amp = 5.0f;

              float eCO2_m = co2_base + co2_amp * sinf(t * 0.1f) + jitter(5.0f);
              float tvoc_m = tvoc_base + tvoc_amp * sinf(t * 0.07f) + jitter(0.1f);
              float lpg_m = lpg_base + lpg_amp * sinf(t * 0.05f) + jitter(3.0f);
              float co_m = co_base + co_amp * sinf(t * 0.12f) + jitter(0.2f);
              float smoke_m = smoke_base + smoke_amp * sinf(t * 0.08f) + jitter(1.0f);
              float alcohol_m = alcohol_base + alcohol_amp * sinf(t * 0.06f) + jitter(0.2f);
              float methane_m = methane_base + methane_amp * sinf(t * 0.04f) + jitter(2.0f);
              float h2_m = h2_base + h2_amp * sinf(t * 0.09f) + jitter(0.5f);

              // Ensure no negative readings
              values[0] = fmaxf(eCO2_m, 0.0f);
              values[1] = fmaxf(tvoc_m, 0.0f);
              values[2] = fmaxf(lpg_m, 0.0f);
              values[3] = fmaxf(co_m, 0.0f);
              values[4] = fmaxf(smoke_m, 0.0f);
              values[5] = fmaxf(alcohol_m, 0.0f);
              values[6] = fmaxf(methane_m, 0.0f);
              values[7] = fmaxf(h2_m, 0.0f);
            }

        // Copy floats to a byte buffer. ESP32 is little-endian, and Android
        // code expects LITTLE_ENDIAN, so a direct memcpy is fine.
        // The Android app expects a 36-byte packet (9 floats). Send 9
        // floats: the original 8 values followed by a placeholder float.
        const size_t payload_floats = 9 * sizeof(float);
        float values9[9];
        for (int i = 0; i < 8; ++i) values9[i] = values[i];
        values9[8] = 0.0f; // placeholder (app computes AQI locally)
        uint8_t outbuf[payload_floats];
        memcpy(outbuf, values9, payload_floats);

        // Send raw bytes over BLE only if a client is connected
        if (clientConnected || pServer->getConnectedCount() > 0) {
          pTxCharacteristic->setValue(outbuf, sizeof(outbuf));
          pTxCharacteristic->notify();
        }

        // Debug print: show values in readable form occasionally to avoid
        // blocking the BLE stack with too much Serial output.
        static int publishCount = 0;
        publishCount++;
        if ((publishCount % 3) == 0) {
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
  }

  // Advertising watchdog: periodically ensure advertising is running when
  // no client is connected. If restarting advertising repeatedly fails,
  // reboot the device to recover the BLE stack.
  static unsigned long lastAdvWatch = 0;
  unsigned long nowWatch = millis();
  if (nowWatch - lastAdvWatch >= ADV_WATCH_INTERVAL) {
    lastAdvWatch = nowWatch;
    if (!clientConnected && pAdv) {
      bool isAdv = pAdv->isAdvertising();
      if (!isAdv) {
        Serial.println("Watchdog: advertising stopped, attempting restart...");
        pAdv->start();
        advRestartAttempts++;
        if (advRestartAttempts > ADV_RESTART_MAX) {
          Serial.println("Watchdog: too many advertising restarts, rebooting...");
          delay(200);
          ESP.restart();
        }
      } else {
        // Advertising is healthy; reset restart counter
        advRestartAttempts = 0;
      }
    }
  }

  delay(20);
}

// Command/JSON handling removed — this sketch only publishes sensor data via BLE notifications
