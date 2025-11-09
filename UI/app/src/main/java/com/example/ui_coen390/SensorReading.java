package com.example.ui_coen390;

public class SensorReading {
    public long timestamp;
    public float co2;
    public float tvoc;
    public float aqi;
    public float propane;
    public float co;
    public float smoke;
    public float alcohol;
    public float methane;
    public float h2;

    public SensorReading(float co2, long timestamp, float tvoc, float propane, float co, float smoke, float alcohol, float methane, float h2, float aqi) {
        this.co2 = co2;
        this.timestamp = timestamp;
        this.tvoc = tvoc;
        this.propane = propane;
        this.co = co;
        this.smoke = smoke;
        this.alcohol = alcohol;
        this.methane = methane;
        this.h2 = h2;
        this.aqi = aqi;
    }

    public float getH2() {
        return h2;
    }

    public void setH2(float h2) {
        this.h2 = h2;
    }

    public float getMethane() {
        return methane;
    }

    public void setMethane(float methane) {
        this.methane = methane;
    }

    public float getAlcohol() {
        return alcohol;
    }

    public void setAlcohol(float alcohol) {
        this.alcohol = alcohol;
    }

    public float getCo() {
        return co;
    }

    public void setCo(float co) {
        this.co = co;
    }

    public float getSmoke() {
        return smoke;
    }

    public void setSmoke(float smoke) {
        this.smoke = smoke;
    }

    public float getAqi() {
        return aqi;
    }

    public void setAqi(float aqi) {
        this.aqi = aqi;
    }

    public float getPropane() {
        return propane;
    }

    public void setPropane(float propane) {
        this.propane = propane;
    }

    public float getTvoc() {
        return tvoc;
    }

    public void setTvoc(float tvoc) {
        this.tvoc = tvoc;
    }

    public float getCo2() {
        return co2;
    }

    public void setCo2(float co2) {
        this.co2 = co2;
    }
}