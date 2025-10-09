# Circuit Diagram

## ESP32 Pin Connections

### CCS811 Air Quality Sensor (I2C Interface)
```
CCS811 Pin  →  ESP32 Pin
---------------------------
VCC         →  3.3V
GND         →  GND
SDA         →  GPIO 21 (I2C SDA)
SCL         →  GPIO 22 (I2C SCL)
WAK         →  GND (optional, for wake mode)
```

### MQ-2 Gas/Smoke Sensor (Analog Interface)
```
MQ-2 Pin    →  ESP32 Pin
---------------------------
VCC         →  5V (or 3.3V if 5V not available)
GND         →  GND
A0          →  GPIO 34 (ADC1_CH6)
```

## Power Supply
- ESP32 can be powered via USB or external 5V power supply
- Ensure adequate current supply (minimum 500mA recommended)

## Notes
1. The CCS811 sensor requires a burn-in period of 48 hours for optimal accuracy
2. The MQ-2 sensor requires a pre-heat time of 24-48 hours for stable readings
3. Keep sensors away from direct airflow for accurate measurements
4. Use a stable power supply to avoid sensor reading fluctuations

## Schematic Overview
```
                    ┌─────────────┐
                    │   ESP32     │
                    │   Dev Board │
                    │             │
    CCS811          │   21 (SDA)  │
    ┌────┐          │   22 (SCL)  │
    │VCC ├──────────┤ 3.3V        │
    │GND ├──────────┤ GND         │
    │SDA ├──────────┤ GPIO 21     │
    │SCL ├──────────┤ GPIO 22     │
    └────┘          │             │
                    │             │
    MQ-2            │   34 (A0)   │
    ┌────┐          │             │
    │VCC ├──────────┤ 5V/3.3V     │
    │GND ├──────────┤ GND         │
    │A0  ├──────────┤ GPIO 34     │
    └────┘          │             │
                    │             │
                    └─────────────┘
```

## Testing
1. Upload the firmware to ESP32
2. Open Serial Monitor at 115200 baud
3. Verify sensor initialization messages
4. Check that sensor readings are being printed
5. Pair ESP32 with Android device
6. Connect via the Android app
