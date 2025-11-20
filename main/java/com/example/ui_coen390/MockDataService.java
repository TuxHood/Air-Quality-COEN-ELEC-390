package com.example.ui_coen390;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Context;

import java.util.Locale;
import android.util.Log;

public class MockDataService extends Service {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private DatabaseHelper myDb;
    private final Runnable mockUpdater = new Runnable() {
        @Override
        public void run() {
            Log.d("MOCK_SVC", "mockUpdater running");
            // Generate simple fake values
            float co2 = 350 + (float) (Math.random() * 300);  // ppm
            float tvoc = 2 + (float) (Math.random() * 40);   // ppb
            float propane = 10 + (float) (Math.random() * 20);   // ppb
            float co = 1 + (float) (Math.random() * 10);   // ppm
            float smoke = 5 + (float) (Math.random() * 15);   // ppb
            float alcohol = 1 + (float) (Math.random() * 5);   // ppb
            float methane = 1 + (float) (Math.random() * 5);   // ppb
            float h2 = 1 + (float) (Math.random() * 5);   // ppb
            float aqi = calcSimpleIndex(co2, tvoc);

            Intent intent = new Intent("mock-data-update");
            intent.putExtra("co2", co2);
            intent.putExtra("tvoc", tvoc);
            intent.putExtra("propane", propane);
            intent.putExtra("co", co);
            intent.putExtra("smoke", smoke);
            intent.putExtra("alcohol", alcohol);
            intent.putExtra("methane", methane);
            intent.putExtra("h2", h2);
            intent.putExtra("aqi", aqi);

            // Persist generated reading to the DB so other screens (Stats) can read it
            if (myDb != null) {
                myDb.insertData(System.currentTimeMillis() / 1000, co2, tvoc, propane, co, smoke, alcohol, methane, h2, aqi);
            }

            // Also save latest values to SharedPreferences (so UI can read immediately)
            long timestamp = System.currentTimeMillis() / 1000;
            Log.d("MOCK_SVC", String.format(Locale.US, "mock update ts=%d co2=%.2f tvoc=%.2f aqi=%.2f", timestamp, co2, tvoc, aqi));
        getSharedPreferences("stats", MODE_PRIVATE).edit()
            .putLong("timestamp", timestamp)
            .putFloat("co2", co2)
            .putFloat("tvoc", tvoc)
            .putFloat("aqi", aqi)
            .putFloat("propane", propane)
            .putFloat("co", co)
            .putFloat("smoke", smoke)
            .putFloat("alcohol", alcohol)
            .putFloat("methane", methane)
            .putFloat("h2", h2)
            .apply();

            LocalBroadcastManager.getInstance(MockDataService.this).sendBroadcast(intent);

            handler.postDelayed(this, 2000); // update every 2 seconds
        }
    };

    private float calcSimpleIndex(float co2, float tvoc) {
        // Simple placeholder for demo (0–500-ish)
        float co2Score = Math.min(500f, co2 / 2f);  // e.g., 1000 ppm -> 500
        float tvocScore = Math.min(500f, tvoc * 5f); // e.g., 100 ppb -> 500
        return Math.max(co2Score, tvocScore);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (myDb == null) myDb = new DatabaseHelper(this);
        handler.post(mockUpdater);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop periodic updates
        handler.removeCallbacks(mockUpdater);
        // Cleanup DB helper reference
        if (myDb != null) {
            myDb = null;
        }
    }
}
