package com.example.ui_coen390;

public class SensorReading {
    private final long timestamp;
    private final float co2;
    private final float tvoc;
    private final float propane;
    private final float co;
    private final float smoke;
    private final float alcohol;
    private final float methane;
    private final float h2;
    private final float aqi;

    public SensorReading(long timestamp, float co2, float tvoc, float propane, float co, float smoke, float alcohol, float methane, float h2, float aqi) {
        this.timestamp = timestamp;
        this.co2 = co2;
        this.tvoc = tvoc;
        this.propane = propane;
        this.co = co;
        this.smoke = smoke;
        this.alcohol = alcohol;
        this.methane = methane;
        this.h2 = h2;
        this.aqi = aqi;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public float getCo2() {
        return co2;
    }

    public float getTvoc() {
        return tvoc;
    }

    public float getPropane() {
        return propane;
    }

    public float getCo() {
        return co;
    }

    public float getSmoke() {
        return smoke;
    }

    public float getAlcohol() {
        return alcohol;
    }

    public float getMethane() {
        return methane;
    }

    public float getH2() {
        return h2;
    }

    public float getAqi() {
        return aqi;
    }
}
