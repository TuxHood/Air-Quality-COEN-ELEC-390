package com.example.ui_coen390;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "SensorData.db";
    public static final String TABLE_NAME = "sensor_readings";
    public static final String COL_1 = "ID";
    public static final String COL_2 = "TIMESTAMP";
    public static final String COL_3 = "CO2";
    public static final String COL_4 = "TVOC";
    public static final String COL_5 = "PROPANE";
    public static final String COL_6 = "CO";
    public static final String COL_7 = "SMOKE";
    public static final String COL_8 = "ALCOHOL";
    public static final String COL_9 = "METHANE";
    public static final String COL_10 = "H2";
    public static final String COL_11 = "AQI";

    public static final String[] POLLUTANT_COLUMNS = {COL_3, COL_4, COL_5, COL_6, COL_7, COL_8, COL_9, COL_10, COL_11};


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + TABLE_NAME + " (ID INTEGER PRIMARY KEY AUTOINCREMENT,TIMESTAMP INTEGER,CO2 REAL,TVOC REAL,PROPANE REAL,CO REAL,SMOKE REAL,ALCOHOL REAL,METHANE REAL,H2 REAL, AQI REAL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean insertData(long timestamp, float co2, float tvoc, float propane, float co, float smoke, float alcohol, float methane, float h2, float aqi) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_2, timestamp);
        contentValues.put(COL_3, co2);
        contentValues.put(COL_4, tvoc);
        contentValues.put(COL_5, propane);
        contentValues.put(COL_6, co);
        contentValues.put(COL_7, smoke);
        contentValues.put(COL_8, alcohol);
        contentValues.put(COL_9, methane);
        contentValues.put(COL_10, h2);
        // If the provided aqi is NaN, compute it once here from pollutant values and store
        float aqiToStore = aqi;
        if (Float.isNaN(aqiToStore)) {
            aqiToStore = calcSimpleIndex(co2, tvoc, co, smoke, propane, methane, alcohol, h2);
        }
        contentValues.put(COL_11, aqiToStore);
        long result = db.insert(TABLE_NAME, null, contentValues);
        return result != -1;
    }

    // --- AQI calculation helpers so DB can compute AQI once when needed ---
    private float scaleToAQI(float value, float threshold) {
        if (threshold <= 0) return 0;
        float scaled = (value / threshold) * 100f;
        if (scaled > 500f) scaled = 500f;   // clamp at hazardous max
        return scaled;
    }

    private float calcSimpleIndex(float co2, float tvoc, float co, float smoke, float propane, float methane, float alcohol, float h2) {
        float CO2_THRESHOLD       = 2000f;   // ppm
        float TVOC_THRESHOLD      = 660f;    // ppb
        float CO_THRESHOLD        = 9f;      // ppm
        float PROPANE_THRESHOLD   = 2100f;   // ppm
        float SMOKE_THRESHOLD     = 150f;    // arbitrary ppm-equivalent from sensor
        float METHANE_THRESHOLD   = 1000f;   // ppm
        float ALCOHOL_THRESHOLD   = 1000f;   // ppm
        float H2_THRESHOLD        = 4100f;   // ppm

        float co2AQI      = scaleToAQI(co2, CO2_THRESHOLD);
        float tvocAQI     = scaleToAQI(tvoc, TVOC_THRESHOLD);
        float coAQI       = scaleToAQI(co, CO_THRESHOLD);
        float propaneAQI  = scaleToAQI(propane, PROPANE_THRESHOLD);
        float smokeAQI    = scaleToAQI(smoke, SMOKE_THRESHOLD);
        float methaneAQI  = scaleToAQI(methane, METHANE_THRESHOLD);
        float alcoholAQI  = scaleToAQI(alcohol, ALCOHOL_THRESHOLD);
        float h2AQI       = scaleToAQI(h2, H2_THRESHOLD);

        float finalAQI = Math.max(
                Math.max(Math.max(co2AQI, tvocAQI),Math.max(coAQI, smokeAQI)),
                Math.max(Math.max(propaneAQI, methaneAQI), Math.max(alcoholAQI, h2AQI))
        );

        return finalAQI;
    }

    public List<Pair<Long, Double>> getReadingsForPollutant(String pollutantColumnName) {
        List<Pair<Long, Double>> dataList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{COL_2, pollutantColumnName}, null, null, null, null, COL_2 + " ASC");

        if (cursor != null) {
            int timestampIndex = cursor.getColumnIndex(COL_2);
            int valueIndex = cursor.getColumnIndex(pollutantColumnName);

            if(timestampIndex != -1 && valueIndex != -1) {
                while (cursor.moveToNext()) {
                    long timestamp = cursor.getLong(timestampIndex);
                    double value = cursor.getDouble(valueIndex);
                    dataList.add(new Pair<>(timestamp, value));
                }
            }
            cursor.close();
        }
        return dataList;
    }
    public ArrayList<SensorReading> getLastNReadings(int n) {
        ArrayList<SensorReading> readings = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " ORDER BY " + COL_1 + " DESC LIMIT " + n, null);

        if (cursor.moveToFirst()) {
            do {
                readings.add(new SensorReading(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_2)),
                        cursor.getFloat(cursor.getColumnIndexOrThrow(COL_3)),
                        cursor.getFloat(cursor.getColumnIndexOrThrow(COL_4)),
                        cursor.getFloat(cursor.getColumnIndexOrThrow(COL_5)),
                        cursor.getFloat(cursor.getColumnIndexOrThrow(COL_6)),
                        cursor.getFloat(cursor.getColumnIndexOrThrow(COL_7)),
                        cursor.getFloat(cursor.getColumnIndexOrThrow(COL_8)),
                        cursor.getFloat(cursor.getColumnIndexOrThrow(COL_9)),
                        cursor.getFloat(cursor.getColumnIndexOrThrow(COL_10)),
                        cursor.getFloat(cursor.getColumnIndexOrThrow(COL_11))
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return readings;
    }

    public ArrayList<SensorReading> getAllReadings() {
        ArrayList<SensorReading> readings = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " ORDER BY " + COL_2 + " ASC", null);

        if (cursor.moveToFirst()) {
            do {
                readings.add(new SensorReading(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_2)),
                        cursor.getFloat(cursor.getColumnIndexOrThrow(COL_3)),
                        cursor.getFloat(cursor.getColumnIndexOrThrow(COL_4)),
                        cursor.getFloat(cursor.getColumnIndexOrThrow(COL_5)),
                        cursor.getFloat(cursor.getColumnIndexOrThrow(COL_6)),
                        cursor.getFloat(cursor.getColumnIndexOrThrow(COL_7)),
                        cursor.getFloat(cursor.getColumnIndexOrThrow(COL_8)),
                        cursor.getFloat(cursor.getColumnIndexOrThrow(COL_9)),
                        cursor.getFloat(cursor.getColumnIndexOrThrow(COL_10)),
                        cursor.getFloat(cursor.getColumnIndexOrThrow(COL_11))
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return readings;
    }
}
