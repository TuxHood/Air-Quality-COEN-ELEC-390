package com.example.ui_coen390;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

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

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + TABLE_NAME + " (ID INTEGER PRIMARY KEY AUTOINCREMENT,TIMESTAMP INTEGER,CO2 REAL,TVOC REAL,PROPANE REAL,CO REAL,SMOKE REAL,ALCOHOL REAL,METHANE REAL,H2 REAL,AQI REAL)");
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
        contentValues.put(COL_11, aqi);
        long result = db.insert(TABLE_NAME, null, contentValues);
        return result != -1;
    }

    public boolean exportToJson(Context context) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        FileWriter fileWriter = null;

        try {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);

            JSONArray jsonArray = new JSONArray();

            if (cursor.moveToFirst()) {
                do {
                    JSONObject obj = new JSONObject();
                    obj.put("ID", cursor.getInt(cursor.getColumnIndexOrThrow("ID")));
                    obj.put("TIMESTAMP", cursor.getLong(cursor.getColumnIndexOrThrow("TIMESTAMP")));
                    obj.put("CO2", cursor.getFloat(cursor.getColumnIndexOrThrow("CO2")));
                    obj.put("TVOC", cursor.getFloat(cursor.getColumnIndexOrThrow("TVOC")));
                    obj.put("PROPANE", cursor.getFloat(cursor.getColumnIndexOrThrow("PROPANE")));
                    obj.put("CO", cursor.getFloat(cursor.getColumnIndexOrThrow("CO")));
                    obj.put("SMOKE", cursor.getFloat(cursor.getColumnIndexOrThrow("SMOKE")));
                    obj.put("ALCOHOL", cursor.getFloat(cursor.getColumnIndexOrThrow("ALCOHOL")));
                    obj.put("METHANE", cursor.getFloat(cursor.getColumnIndexOrThrow("METHANE")));
                    obj.put("H2", cursor.getFloat(cursor.getColumnIndexOrThrow("H2")));
                    obj.put("AQI", cursor.getFloat(cursor.getColumnIndexOrThrow("AQI")));

                    jsonArray.put(obj);
                } while (cursor.moveToNext());
            }

            // Write JSON to internal storage
            File file = new File(context.getFilesDir(), "sensor_data_export.json");
            fileWriter = new FileWriter(file);
            fileWriter.write(jsonArray.toString(4)); // Pretty-print with indentation
            fileWriter.close();

            Log.d("DB_EXPORT", "Data exported to: " + file.getAbsolutePath());
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("DB_EXPORT", "Export failed", e);
            return false;
        } finally {
            if (cursor != null) cursor.close();
            if (fileWriter != null) {
                try { fileWriter.close(); } catch (Exception ignored) {}
            }
        }
    }

    public ArrayList<SensorReading> getLastNReadings(int n) {
        ArrayList<SensorReading> readings = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT TIMESTAMP, CO2, TVOC, PROPANE, CO, SMOKE, ALCOHOL, METHANE, H2, AQI FROM " + TABLE_NAME +
                " ORDER BY ID DESC LIMIT " + n;
        android.database.Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("TIMESTAMP"));
                float aqi = cursor.getFloat(cursor.getColumnIndexOrThrow("AQI"));
                float co2 = cursor.getFloat(cursor.getColumnIndexOrThrow("CO2"));
                float tvoc = cursor.getFloat(cursor.getColumnIndexOrThrow("TVOC"));
                float propane = cursor.getFloat(cursor.getColumnIndexOrThrow("PROPANE"));
                float co = cursor.getFloat(cursor.getColumnIndexOrThrow("CO"));
                float smoke = cursor.getFloat(cursor.getColumnIndexOrThrow("SMOKE"));
                float alcohol = cursor.getFloat(cursor.getColumnIndexOrThrow("ALCOHOL"));
                float methane = cursor.getFloat(cursor.getColumnIndexOrThrow("METHANE"));
                float h2 = cursor.getFloat(cursor.getColumnIndexOrThrow("H2"));

                readings.add(new SensorReading( co2,  timestamp,  tvoc,  propane,  co,  smoke,  alcohol,  methane,  h2, aqi));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        // reverse order so chart shows oldest → newest
        java.util.Collections.reverse(readings);

        return readings;
    }

}
