package com.example.ui_coen390;

public class Pollutant {
    private String name;
    private String value;

    public Pollutant(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
