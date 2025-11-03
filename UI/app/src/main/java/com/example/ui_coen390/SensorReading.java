package com.example.ui_coen390;

public class SensorReading {
    public long timestamp;
    public float co2;
    public float tvoc;
    public float aqi;

    public SensorReading(long timestamp, float co2, float tvoc, float aqi) {
        this.timestamp = timestamp;
        this.co2 = co2;
        this.tvoc = tvoc;
        this.aqi = aqi;
    }
}