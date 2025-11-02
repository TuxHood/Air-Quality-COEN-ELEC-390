package com.example.ui_coen390;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
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
        contentValues.put(COL_11, aqi);
        long result = db.insert(TABLE_NAME, null, contentValues);
        return result != -1;
    }

    public List<Pair<Long, Double>> getReadingsForPollutant(String pollutantColumnName) {
        List<Pair<Long, Double>> dataList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        // Querying the database, ordering by timestamp to plot the graph correctly
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
}
