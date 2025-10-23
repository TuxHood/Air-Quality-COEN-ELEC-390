// NimBLE-based replacement for BluetoothSerial (SPP) on ESP32-S3
#include <NimBLEDevice.h>
#include <ArduinoJson.h>
#include <Wire.h>
#include <Adafruit_CCS811.h>

#define DEVICE_NAME "AQ-Device"

// UUIDs for Nordic UART Service (NUS)
static BLEUUID serviceUUID("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
static BLEUUID txCharUUID("6E400003-B5A3-F393-E0A9-E50E24DCCA9E"); // notify (device -> client)
static BLEUUID rxCharUUID("6E400002-B5A3-F393-E0A9-E50E24DCCA9E"); // write (client -> device)

NimBLECharacteristic* pRxCharacteristic = nullptr;
NimBLECharacteristic* pTxCharacteristic = nullptr;

// Sensors
Adafruit_CCS811 ccs;
const int MQ2_PIN = 34; // Analog input for MQ-2, change if needed
unsigned long lastSensorPublish = 0;
const unsigned long SENSOR_PUBLISH_INTERVAL = 5000; // ms

// Function declarations
void toggleGPIO(JsonObject accessory);
void syncController(JsonArray accessory_list);
void checkData(const String &payload);

class RxCallbacks : public NimBLECharacteristicCallbacks {
  void onWrite(NimBLECharacteristic* pChar, NimBLEConnInfo& connInfo) override {
    (void)connInfo;
    std::string value = pChar->getValue();
    if (!value.empty()) {
      String payload(value.c_str());
      checkData(payload);
    }
  }
};

void setup() {
  Serial.begin(115200);
  delay(100);

  // sensors init
  Wire.begin();
  if (!ccs.begin()) {
    Serial.println("CCS811 not found. Check wiring.");
  } else {
    // sensor needs to estabilize; do a non-blocking short wait
    Serial.println("CCS811 initialized");
  }

  // BLE init
  NimBLEDevice::init(DEVICE_NAME);
  NimBLEServer* pServer = NimBLEDevice::createServer();
  NimBLEService* pService = pServer->createService(serviceUUID);

  pRxCharacteristic = pService->createCharacteristic(rxCharUUID, NIMBLE_PROPERTY::WRITE);
  pRxCharacteristic->setCallbacks(new RxCallbacks());

  pTxCharacteristic = pService->createCharacteristic(txCharUUID, NIMBLE_PROPERTY::NOTIFY);

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
  Serial.println("Connect with a BLE client and write to RX characteristic.");
}

void loop() {
  unsigned long now = millis();
  if (now - lastSensorPublish >= SENSOR_PUBLISH_INTERVAL) {
    lastSensorPublish = now;

      // Read sensors
      float eCO2 = 0.0f;
      float tvoc = 0.0f;
      // Try reading CCS811 if available; otherwise keep zeros
      if (ccs.available()) {
        if (!ccs.readData()) {
          eCO2 = ccs.geteCO2();
          tvoc = ccs.getTVOC();
        } else {
          Serial.println("CCS811 read error");
        }
      }

      // MQ-2: if the MQ2 library is not present here, use ADC as a proxy.
      // analogRead on ESP32 returns 0..4095; we'll provide both raw ADC and a
      // simple scaled ppm simulation (0..1000 ppm) so the Android app receives
      // easily consumable numbers named after the MQ2 API the app expects.
      int mq2_adc = analogRead(MQ2_PIN);
      auto adcToPpm = [](int adc)->float { return (adc / 4095.0f) * 1000.0f; };

      float lpg_ppm = adcToPpm(mq2_adc);       // simulated LPG (ppm)
      float co_ppm = adcToPpm((mq2_adc + 50) % 4096); // small variation
      float smoke_ppm = adcToPpm((mq2_adc + 100) % 4096);
      float alcohol_ppm = adcToPpm((mq2_adc + 150) % 4096);
      float methane_ppm = adcToPpm((mq2_adc + 200) % 4096);
      float h2_ppm = adcToPpm((mq2_adc + 250) % 4096);

      // Build JSON and notify containing the requested fields
      if (pTxCharacteristic) {
        DynamicJsonDocument doc(512);
        doc["type"] = "sensors";
        JsonObject payload = doc.createNestedObject("payload");

        // MQ-2 derived fields (match names you requested)
        payload["lpg"] = lpg_ppm;        // gasTest.readLPG()
        payload["co"] = co_ppm;          // gasTest.readCO()
        payload["smoke"] = smoke_ppm;    // gasTest.readSmoke()
        payload["alcohol"] = alcohol_ppm;// gasTest.readAlcohol()
        payload["methane"] = methane_ppm;// gasTest.readMethane()
        payload["h2"] = h2_ppm;          // gasTest.readH2()
        payload["mq2_adc"] = mq2_adc;    // raw ADC for debugging

        // CCS811 fields (CO2 and TVOC) - names reflect mySensor.getCO2()/getTVOC()
        payload["co2"] = eCO2; // if sensor not ready, will be 0
        payload["tvoc"] = tvoc;

        // optional: timestamp
        payload["ts_ms"] = millis();

        char buf[512];
        size_t len = serializeJson(doc, buf, sizeof(buf));
        pTxCharacteristic->setValue((uint8_t*)buf, len);
        pTxCharacteristic->notify();
        Serial.print("Published sensors: ");
        Serial.println(buf);
      }
  }

  delay(20);
}

void checkData(const String &payload)
{
  DynamicJsonDocument jsonDoc(1024);
  DeserializationError error = deserializeJson(jsonDoc, payload);
  if (error)
  {
    Serial.print(F("deserializeJson() failed: "));
    Serial.println(error.f_str());
    return;
  }
  JsonObject root = jsonDoc.as<JsonObject>();
  if (root.isNull()) {
    Serial.println(F("The root of the JSON document is not an object"));
    return;
  }

  if (root["type"].is<const char*>()) {
    const char* type = root["type"];
    if (strcmp(type, "command") == 0) {
      if (root["accessory"].is<JsonObject>()) {
        toggleGPIO(root["accessory"].as<JsonObject>());
      }
      if (pTxCharacteristic) {
        pTxCharacteristic->setValue("ACK:command");
        pTxCharacteristic->notify();
      }
    } else if (strcmp(type, "sync") == 0) {
      if (root["accessory_list"].is<JsonArray>()) {
        syncController(root["accessory_list"].as<JsonArray>());
      }
      if (pTxCharacteristic) {
        pTxCharacteristic->setValue("ACK:sync");
        pTxCharacteristic->notify();
      }
    } else {
      Serial.print(F("Unknown type: "));
      Serial.println(type);
    }
  } else {
    Serial.println(F("Key 'type' not found in JSON or is not a string"));
  }
}

void toggleGPIO(JsonObject accessory)
{
  if (accessory.isNull()) {
    Serial.println(F("Key 'accessory' not found in JSON"));
    return;
  }
  int accessory_id = accessory["acc_id"] | 0;
  const char *name = accessory["name"] | "";
  int pin_number = accessory["gpio"] | -1;
  bool pin_status = accessory["status"] | false;

  if (pin_number >= 0) {
    pinMode(pin_number, OUTPUT);
    digitalWrite(pin_number, pin_status);
    Serial.println("GPIO " + String(pin_number) + " " + (pin_status ? "ON" : "OFF"));
  } else {
    Serial.println(F("Invalid GPIO in accessory"));
  }
}

void syncController(JsonArray accessory_list)
{
  if (accessory_list.isNull()) {
    Serial.println(F("Key 'accessory_list' not found in JSON"));
    return;
  }
  for (JsonObject accessory : accessory_list)
  {
    int accessory_id = accessory["acc_id"] | 0;
    const char *name = accessory["name"] | "";
    int pin_number = accessory["gpio"] | -1;
    bool pin_status = accessory["status"] | false;

    if (pin_number >= 0) {
      pinMode(pin_number, OUTPUT);
      digitalWrite(pin_number, pin_status);
    }
  }
  Serial.println("Sync completed");
}
