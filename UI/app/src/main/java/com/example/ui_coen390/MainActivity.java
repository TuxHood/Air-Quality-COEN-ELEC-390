package com.example.ui_coen390;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BLE_DEBUG";
    private static final int PERMISSION_REQUEST_CODE = 101;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean scanning;
    private static final long SCAN_PERIOD = 10000;

    //IMPORTANT REPLACE MAC ADDRESS WITH YOUR DEVICE
    private static final String DEVICE_ADDRESS = "e8:6b:ea:c9:ed:e2";

    // UUIDs for service/characteristic (update if needed)
    private static final UUID SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");

    private Button connectButton;
    private DatabaseHelper myDb;
    private RecyclerView pollutantsRecyclerView;
    private PollutantAdapter pollutantAdapter;
    private List<Pollutant> pollutantList;
    private TextView AQIStatementTextView;

    private static final boolean MOCK_MODE = true;

    // --- START: Added for Air Quality Alerts ---
    private final Map<String, Long> lastAlertTimestamps = new HashMap<>();
    private static final long ALERT_COOLDOWN = 5 * 60 * 100; // 30 seconds in milliseconds
    // --- END: Added for Air Quality Alerts ---

    // Receiver to get mock updates from MockDataService
    private BroadcastReceiver mockReceiver;
    private final Runnable statsPoller = new Runnable() {
        @Override
        public void run() {
            try {
                android.content.SharedPreferences stats = getSharedPreferences("stats", MODE_PRIVATE);
                if (stats.contains("co2") || stats.contains("tvoc") || stats.contains("aqi")) {
                    float co2 = stats.getFloat("co2", Float.NaN);
                    float tvoc = stats.getFloat("tvoc", Float.NaN);
                    float propane = stats.getFloat("propane", Float.NaN);
                    float co = stats.getFloat("co", Float.NaN);
                    float smoke = stats.getFloat("smoke", Float.NaN);
                    float alcohol = stats.getFloat("alcohol", Float.NaN);
                    float methane = stats.getFloat("methane", Float.NaN);
                    float h2 = stats.getFloat("h2", Float.NaN);
                    float aqi = stats.getFloat("aqi", Float.NaN);

                    checkAirQualityLevels(co2, tvoc, co, smoke, propane, methane, alcohol, h2);

                    for (Pollutant pollutant : pollutantList) {
                        switch (pollutant.getName()) {
                            case "CO2":
                                pollutant.setValue(String.format(Locale.US, "%.2f ppm", co2));
                                break;
                            case "TVOC":
                                pollutant.setValue(String.format(Locale.US, "%.2f ppb", tvoc));
                                break;
                            case "AQI":
                                pollutant.setValue(String.format(Locale.US, "%.2f", aqi));
                                break;
                            case "Propane":
                                pollutant.setValue(String.format(Locale.US, "%.2f ppb", propane));
                                break;
                            case "CO":
                                pollutant.setValue(String.format(Locale.US, "%.2f ppm", co));
                                break;
                            case "Smoke":
                                pollutant.setValue(String.format(Locale.US, "%.2f ppb", smoke));
                                break;
                            case "Alcohol":
                                pollutant.setValue(String.format(Locale.US, "%.2f ppb", alcohol));
                                break;
                            case "Methane":
                                pollutant.setValue(String.format(Locale.US, "%.2f ppb", methane));
                                break;
                            case "H2":
                                pollutant.setValue(String.format(Locale.US, "%.2f ppb", h2));
                                break;
                        }
                    }
                    pollutantAdapter.notifyDataSetChanged();
                }
            } catch (Exception ignored) {}
            // schedule next poll
            handler.postDelayed(this, 1000);
        }
    };

    private final Runnable mockUpdater = new Runnable() {
        @Override public void run() {
            // Generate simple fake values
            float co2  = 350 + (float)(Math.random() * 300);  // ppm
            float tvoc =  2  + (float)(Math.random() * 40);   // ppb
            float propane = 10 + (float)(Math.random() * 20);   // ppb
            float co = 1 + (float)(Math.random() * 10);   // ppm
            float smoke = 5 + (float)(Math.random() * 15);   // ppb
            float alcohol = 1 + (float)(Math.random() * 5);   // ppb
            float methane = 1 + (float)(Math.random() * 5);   // ppb
            float h2 = 1 + (float)(Math.random() * 5);   // ppb
            float aqi = calcSimpleIndex(co2, tvoc, co, smoke, propane, methane, alcohol, h2);

            checkAirQualityLevels(co2, tvoc, co, smoke, propane, methane, alcohol, h2);

            long timestamp = System.currentTimeMillis() / 1000;
            myDb.insertData(timestamp, co2, tvoc, propane, co, smoke, alcohol, methane, h2, aqi);

            runOnUiThread(() -> {
                for (Pollutant pollutant : pollutantList) {
                    switch (pollutant.getName()) {
                        case "CO2":
                            pollutant.setValue(String.format(Locale.US, "%.2f ppm", co2));
                            break;
                        case "TVOC":
                            pollutant.setValue(String.format(Locale.US, "%.2f ppb", tvoc));
                            break;
                        case "AQI":
                            pollutant.setValue(String.format(Locale.US, "%.2f", aqi));
                            break;
                        case "Propane":
                            pollutant.setValue(String.format(Locale.US, "%.2f ppb", propane));
                            break;
                        case "CO":
                            pollutant.setValue(String.format(Locale.US, "%.2f ppm", co));
                            break;
                        case "Smoke":
                            pollutant.setValue(String.format(Locale.US, "%.2f ppb", smoke));
                            break;
                        case "Alcohol":
                            pollutant.setValue(String.format(Locale.US, "%.2f ppb", alcohol));
                            break;
                        case "Methane":
                            pollutant.setValue(String.format(Locale.US, "%.2f ppb", methane));
                            break;
                        case "H2":
                            pollutant.setValue(String.format(Locale.US, "%.2f ppb", h2));
                            break;
                    }
                }
                pollutantAdapter.notifyDataSetChanged();
            });

            handler.postDelayed(this, 2000); // update every 2 seconds
        }
    };

    private void startMockMode() {
        runOnUiThread(() -> {
            for (Pollutant pollutant : pollutantList) {
                pollutant.setValue("Connecting...");
            }
            pollutantAdapter.notifyDataSetChanged();
            connectButton.setEnabled(false);
        });

        // Start background service that generates mock data and writes to DB
        Intent svc = new Intent(this, MockDataService.class);
        startService(svc);

        // Register a local broadcast receiver to receive UI updates from the service
        if (mockReceiver == null) {
            mockReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    // Update pollutant values on the UI thread
                    runOnUiThread(() -> {
                        float co2 = intent.getFloatExtra("co2", Float.NaN);
                        float tvoc = intent.getFloatExtra("tvoc", Float.NaN);
                        float propane = intent.getFloatExtra("propane", Float.NaN);
                        float co = intent.getFloatExtra("co", Float.NaN);
                        float smoke = intent.getFloatExtra("smoke", Float.NaN);
                        float alcohol = intent.getFloatExtra("alcohol", Float.NaN);
                        float methane = intent.getFloatExtra("methane", Float.NaN);
                        float h2 = intent.getFloatExtra("h2", Float.NaN);
                        float aqi = intent.getFloatExtra("aqi", Float.NaN);

                        checkAirQualityLevels(co2, tvoc, co, smoke, propane, methane, alcohol, h2);
                        calcSimpleIndex(co2, tvoc, co, smoke, propane, methane, alcohol, h2);


                        for (Pollutant pollutant : pollutantList) {
                            switch (pollutant.getName()) {
                                case "CO2":
                                    pollutant.setValue(String.format(Locale.US, "%.2f ppm", co2));
                                    break;
                                case "TVOC":
                                    pollutant.setValue(String.format(Locale.US, "%.2f ppb", tvoc));
                                    break;
                                case "AQI":
                                    pollutant.setValue(String.format(Locale.US, "%.2f", aqi));
                                    break;
                                case "Propane":
                                    pollutant.setValue(String.format(Locale.US, "%.2f ppb", propane));
                                    break;
                                case "CO":
                                    pollutant.setValue(String.format(Locale.US, "%.2f ppm", co));
                                    break;
                                case "Smoke":
                                    pollutant.setValue(String.format(Locale.US, "%.2f ppb", smoke));
                                    break;
                                case "Alcohol":
                                    pollutant.setValue(String.format(Locale.US, "%.2f ppb", alcohol));
                                    break;
                                case "Methane":
                                    pollutant.setValue(String.format(Locale.US, "%.2f ppb", methane));
                                    break;
                                case "H2":
                                    pollutant.setValue(String.format(Locale.US, "%.2f ppb", h2));
                                    break;
                            }
                        }
                        pollutantAdapter.notifyDataSetChanged();
                        connectButton.setText(R.string.status_connected);
                        connectButton.setEnabled(true);
                    });
                }
            };
            LocalBroadcastManager.getInstance(this).registerReceiver(mockReceiver, new IntentFilter("mock-data-update"));
        }

        // Start polling SharedPreferences for latest stats as a fallback to updates
        handler.removeCallbacks(statsPoller);
        handler.post(statsPoller);
    }

    // --- START: Added for Air Quality Alerts ---
    private void showAirQualityAlert(String pollutantName, String level, String recommendation) {
        // Ensure this runs on the UI thread
        runOnUiThread(() -> {
            new AlertDialog.Builder(this)
                    .setTitle("Air Quality Alert: High " + pollutantName)
                    .setMessage("Current level: " + level + ".\n\nRecommendation: " + recommendation)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .create()
                    .show();
        });
    }

    private void checkAirQualityLevels(float co2, float tvoc, float co, float smoke, float propane, float methane, float alcohol, float h2) {
        long currentTime = System.currentTimeMillis();

        // --- Check TVOC Levels ---
        // Source: U.S. Environmental Protection Agency (EPA) and various indoor air quality studies.
        // > 660 ppb is considered high and may cause irritation.
        if (tvoc > 660) {
            if (!lastAlertTimestamps.containsKey("TVOC") || (currentTime - lastAlertTimestamps.get("TVOC") > ALERT_COOLDOWN)) {
                lastAlertTimestamps.put("TVOC", currentTime);
                showAirQualityAlert("TVOC", String.format(Locale.US, "%.2f ppb", tvoc), "Levels are high. Consider ventilating the area by opening a window or using an air purifier.");
            }
        }

        // --- Check CO2 Levels ---
        // Source: ASHRAE Standard 62.1.
        // > 1000 ppm can lead to complaints of drowsiness and poor air quality.
        // > 2000 ppm can cause headaches and decreased cognitive function.
        if (co2 > 2000) {
            if (!lastAlertTimestamps.containsKey("CO2") || (currentTime - lastAlertTimestamps.get("CO2") > ALERT_COOLDOWN)) {
                lastAlertTimestamps.put("CO2", currentTime);
                showAirQualityAlert("CO2", String.format(Locale.US, "%.2f ppm", co2), "Levels are very high, which can impact concentration and cause headaches. It is strongly recommended to get fresh air.");
            }
        }

        // --- Check CO Levels ---
        // Source: World Health Organization (WHO), EPA.
        // > 9 ppm is the 8-hour exposure limit. Any sustained level above this is a concern.
        if (co > 9) {
            if (!lastAlertTimestamps.containsKey("CO") || (currentTime - lastAlertTimestamps.get("CO") > ALERT_COOLDOWN)) {
                lastAlertTimestamps.put("CO", currentTime);
                showAirQualityAlert("CO (Carbon Monoxide)", String.format(Locale.US, "%.2f ppm", co), "Potentially dangerous levels detected. Carbon Monoxide is hazardous. Ventilate the area immediately and consider leaving the room.");
            }
        }

        // --- Check Smoke Levels ---
        // Source: https://www.healthline.com/health-news/how-to-tell-if-the-air-is-safe-enough-to-exercise-outside
        // > 150, The air is unhealthy for sensitive groups and people with conditions. Some people in general public may also experience health effects
        // > 200, Risk factors are increased for everyone
        if (smoke > 150) {
            if (!lastAlertTimestamps.containsKey("Smoke") || (currentTime - lastAlertTimestamps.get("Smoke") > ALERT_COOLDOWN)) {
                lastAlertTimestamps.put("Smoke", currentTime);
                showAirQualityAlert("Smoke", String.format(Locale.US, "%.2f ppb", smoke), "Levels of smoke are high. Consider opening windows and chekcing for smole sources.");
            }
        }

        // --- Check Propane Levels ---
        // Source: https://www.cdc.gov/niosh/idlh/74986.html
        if (propane > 2100) {
            if (!lastAlertTimestamps.containsKey("Propane") || (currentTime - lastAlertTimestamps.get("Propane") > ALERT_COOLDOWN)) {
                lastAlertTimestamps.put("Propane", currentTime);
                showAirQualityAlert("Propane", String.format(Locale.US, "%.2f ppm", propane), "Levels of propane are high. Increase ventilation by opening windows and door and inspect common sources such as Stove burners, gas heaters, barbecue tanks. Avoid using lighters, candles or other open flames.");
            }
        }

        // --- Check Methane Levels ---
        // Source: https://minearc.com/wp-content/uploads/2021/04/risks-and-safety-hazards-of-methane-infographic-final.pdf
        // Recommended a maximum of 1000ppm during an eight-hour work period
        if (methane > 1000) {
            if (!lastAlertTimestamps.containsKey("Methane") || (currentTime - lastAlertTimestamps.get("Methane") > ALERT_COOLDOWN)) {
                lastAlertTimestamps.put("Methane", currentTime);
                showAirQualityAlert("Methane", String.format(Locale.US, "%.2f ppm", methane), "Potentially dangerous levels detected. Ventilate the area and inspect nearby gas sources. Consider leaving the room");
            }
        }

        // --- Check Alcohol Levels ---
        // Source: https://www.osha.gov/chemicaldata/1034
        // Exposure limit of 1000 ppm
        if (alcohol > 1000) {
            if (!lastAlertTimestamps.containsKey("Alcohol") || (currentTime - lastAlertTimestamps.get("Alcohol") > ALERT_COOLDOWN)) {
                lastAlertTimestamps.put("Alcohol", currentTime);
                showAirQualityAlert("Alcohol", String.format(Locale.US, "%.2f ppm", alcohol), "Levels of alcohol are high. Consider leaving the room and ventilating the area.");
            }
        }

        // --- Check H2 Levels ---
        // Source: https://nap.nationalacademies.org/read/12032/chapter/9
        // While not toxic, H2 is very flammable
        if (h2 > 4100) {
            if (!lastAlertTimestamps.containsKey("H2") || (currentTime - lastAlertTimestamps.get("H2") > ALERT_COOLDOWN)) {
                lastAlertTimestamps.put("H2", currentTime);
                showAirQualityAlert("H2", String.format(Locale.US, "%.2f ppm", h2), "Levels of H2 are high. Ventilate area and turn off ignition sources. Move away from area if smell of leak is suspected");
            }
        }

    }
    // --- END: Added for Air Quality Alerts ---

    // --- SCALE AQI: 0–500 ---
    // AQI Categories:
    // 0–50 Good
    // 51–100 Moderate
    // 101–200 Unhealthy
    // 201–300 Very Unhealthy
    // 301–500 Hazardous

    private float scaleToAQI(float value, float threshold) {
        if (threshold <= 0) return 0;
        float scaled = (value / threshold) * 100f;
        if (scaled > 500f) scaled = 500f;   // clamp at hazardous max
        return scaled;
    }

    public float calcSimpleIndex(float co2, float tvoc, float co, float smoke, float propane, float methane, float alcohol, float h2) {

        // --- POLLUTANT THRESHOLDS ---
        // Based on common indoor standards + typical sensor alert levels
        float CO2_THRESHOLD       = 2000f;   // ppm
        float TVOC_THRESHOLD      = 660f;    // ppb
        float CO_THRESHOLD        = 9f;      // ppm
        float PROPANE_THRESHOLD   = 2100f;     // ppm
        float SMOKE_THRESHOLD     = 150f;    // arbitrary ppm-equivalent from sensor
        float METHANE_THRESHOLD   = 1000f;  // ppm
        float ALCOHOL_THRESHOLD   = 1000f;     // ppm (typical semiconductor alarm level)
        float H2_THRESHOLD        = 4100f;   // ppm  (EPA/NIOSH recommended limit)

        // --- SCALE EACH POLLUTANT ---
        float co2AQI      = scaleToAQI(co2, CO2_THRESHOLD);
        float tvocAQI     = scaleToAQI(tvoc, TVOC_THRESHOLD);
        float coAQI       = scaleToAQI(co, CO_THRESHOLD);
        float propaneAQI  = scaleToAQI(propane, PROPANE_THRESHOLD);
        float smokeAQI    = scaleToAQI(smoke, SMOKE_THRESHOLD);
        float methaneAQI  = scaleToAQI(methane, METHANE_THRESHOLD);
        float alcoholAQI  = scaleToAQI(alcohol, ALCOHOL_THRESHOLD);
        float h2AQI       = scaleToAQI(h2, H2_THRESHOLD);

        // --- FINAL AQI = WORST POLLUTANT ---
        float finalAQI = Math.max(
                Math.max(Math.max(co2AQI, tvocAQI),Math.max(coAQI, smokeAQI)),
                Math.max(Math.max(propaneAQI, methaneAQI), Math.max(alcoholAQI, h2AQI))
        );

        AQIStatementTextView.setVisibility(View.VISIBLE);

        if (0 <= finalAQI && finalAQI <= 50){
            AQIStatementTextView.setText("The AQI is " + Math.round(finalAQI) + ". The air quality is good.");
        }
        else if (51 <= finalAQI && finalAQI <= 100){
            AQIStatementTextView.setText("The AQI is " + Math.round(finalAQI) + ". The air quality is moderate.");
        }
        else if (101 <= finalAQI && finalAQI <= 200){
            AQIStatementTextView.setText("The AQI is " + Math.round(finalAQI) + ". The air quality is unhealthy.");
        }
        else if (201 <= finalAQI && finalAQI <= 300){
            AQIStatementTextView.setText("The AQI is " + Math.round(finalAQI) + ". The air quality is very unhealthy.");
        }
        else if (301 <= finalAQI && finalAQI <= 500){
            AQIStatementTextView.setText("The AQI is " + Math.round(finalAQI) + ". The air quality is hazardous.");
        }

        return finalAQI;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        myDb = new DatabaseHelper(this);
        connectButton = findViewById(R.id.homeButton);

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        connectButton.setOnClickListener(v -> {
            Log.d(TAG, "Connect button pressed.");
            if (MOCK_MODE) {
                Log.d(TAG, "Demo mode: starting fake updates.");
                startMockMode();
            } else {
                handleConnectionRequest();  // real BLE path
            }
        });

        setupRecyclerView();

        AQIStatementTextView = findViewById(R.id.AQIStatementTextView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the list of pollutants when returning from settings
        // Preserve existing values where possible instead of resetting to "Disconnected"
        List<Pollutant> existing = new ArrayList<>(pollutantList);
        List<Pollutant> updated = getPollutantsFromSettings();
        for (Pollutant p : updated) {
            // try to preserve value from existing list
            boolean found = false;
            for (Pollutant old : existing) {
                if (old.getName().equals(p.getName())) {
                    p.setValue(old.getValue());
                    found = true;
                    break;
                }
            }
            if (!found) {
                p.setValue("Disconnected");
            }
        }
        pollutantList.clear();
        pollutantList.addAll(updated);
        pollutantAdapter.notifyDataSetChanged();

        // Try to populate UI immediately from last saved stats so we don't show "Connecting..." forever
        try {
            android.content.SharedPreferences stats = getSharedPreferences("stats", MODE_PRIVATE);
            boolean hasAny = stats.contains("co2") || stats.contains("tvoc") || stats.contains("aqi");
            if (hasAny) {
                float co2 = stats.getFloat("co2", Float.NaN);
                float tvoc = stats.getFloat("tvoc", Float.NaN);
                float propane = stats.getFloat("propane", Float.NaN);
                float co = stats.getFloat("co", Float.NaN);
                float smoke = stats.getFloat("smoke", Float.NaN);
                float alcohol = stats.getFloat("alcohol", Float.NaN);
                float methane = stats.getFloat("methane", Float.NaN);
                float h2 = stats.getFloat("h2", Float.NaN);
                float aqi = stats.getFloat("aqi", Float.NaN);

                checkAirQualityLevels(co2, tvoc, co, smoke, propane, methane, alcohol, h2);

                for (Pollutant pollutant : pollutantList) {
                    switch (pollutant.getName()) {
                        case "CO2":
                            pollutant.setValue(String.format(Locale.US, "%.2f ppm", co2));
                            break;
                        case "TVOC":
                            pollutant.setValue(String.format(Locale.US, "%.2f ppb", tvoc));
                            break;
                        case "AQI":
                            pollutant.setValue(String.format(Locale.US, "%.2f", aqi));
                            break;
                        case "Propane":
                            pollutant.setValue(String.format(Locale.US, "%.2f ppb", propane));
                            break;
                        case "CO":
                            pollutant.setValue(String.format(Locale.US, "%.2f ppm", co));
                            break;
                        case "Smoke":
                            pollutant.setValue(String.format(Locale.US, "%.2f ppb", smoke));
                            break;
                        case "Alcohol":
                            pollutant.setValue(String.format(Locale.US, "%.2f ppb", alcohol));
                            break;
                        case "Methane":
                            pollutant.setValue(String.format(Locale.US, "%.2f ppb", methane));
                            break;
                        case "H2":
                            pollutant.setValue(String.format(Locale.US, "%.2f ppb", h2));
                            break;
                    }
                }
                pollutantAdapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            // ignore and fall back to broadcast updates
        }

        // If SharedPreferences didn't provide values (or some values are NaN), try reading the latest DB row as a fallback
        boolean needsDbFallback = false;
        for (Pollutant p : pollutantList) {
            String v = p.getValue();
            if (v == null || v.isEmpty() || v.equals("Disconnected") || v.equals("Connecting...") || v.contains("NaN")) {
                needsDbFallback = true;
                break;
            }
        }

        if (needsDbFallback) {
            try {
                java.util.ArrayList<SensorReading> last = myDb.getLastNReadings(1);
                if (last != null && !last.isEmpty()) {
                    SensorReading r = last.get(0);

                    checkAirQualityLevels(r.getCo2(), r.getTvoc(), r.getCo(), r.getSmoke(), r.getPropane(), r.getMethane(), r.getAlcohol(), r.getH2());

                    for (Pollutant pollutant : pollutantList) {
                        switch (pollutant.getName()) {
                            case "CO2":
                                pollutant.setValue(String.format(Locale.US, "%.2f ppm", r.getCo2()));
                                break;
                            case "TVOC":
                                pollutant.setValue(String.format(Locale.US, "%.2f ppb", r.getTvoc()));
                                break;
                            case "AQI":
                                pollutant.setValue(String.format(Locale.US, "%.2f", r.getAqi()));
                                break;
                            case "Propane":
                                pollutant.setValue(String.format(Locale.US, "%.2f ppb", r.getPropane()));
                                break;
                            case "CO":
                                pollutant.setValue(String.format(Locale.US, "%.2f ppm", r.getCo()));
                                break;
                            case "Smoke":
                                pollutant.setValue(String.format(Locale.US, "%.2f ppb", r.getSmoke()));
                                break;
                            case "Alcohol":
                                pollutant.setValue(String.format(Locale.US, "%.2f ppb", r.getAlcohol()));
                                break;
                            case "Methane":
                                pollutant.setValue(String.format(Locale.US, "%.2f ppb", r.getMethane()));
                                break;
                            case "H2":
                                pollutant.setValue(String.format(Locale.US, "%.2f ppb", r.getH2()));
                                break;
                        }
                    }
                    pollutantAdapter.notifyDataSetChanged();
                }
            } catch (Exception ex) {
                // ignore fallback failure
            }
        }

        // Note: mockReceiver registration happens only when startMockMode() is invoked
    }

    private void setupRecyclerView() {
        pollutantsRecyclerView = findViewById(R.id.pollutantsRecyclerView);
        pollutantsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        pollutantList = getPollutantsFromSettings();
        pollutantAdapter = new PollutantAdapter(pollutantList);
        pollutantsRecyclerView.setAdapter(pollutantAdapter);
    }

    private List<Pollutant> getPollutantsFromSettings() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        Set<String> selectedPollutants = prefs.getStringSet("selectedPollutants", null);
        List<Pollutant> pollutants = new ArrayList<>();

        // Always add AQI at the top
        pollutants.add(new Pollutant("AQI", "Disconnected"));

        if (selectedPollutants == null) {
            // If no settings are saved, add all pollutants by default
            pollutants.add(new Pollutant("CO2", "Disconnected"));
            pollutants.add(new Pollutant("TVOC", "Disconnected"));
            pollutants.add(new Pollutant("Propane", "Disconnected"));
            pollutants.add(new Pollutant("CO", "Disconnected"));
            pollutants.add(new Pollutant("Smoke", "Disconnected"));
            pollutants.add(new Pollutant("Alcohol", "Disconnected"));
            pollutants.add(new Pollutant("Methane", "Disconnected"));
            pollutants.add(new Pollutant("H2", "Disconnected"));
        } else {
            for (String pollutantName : selectedPollutants) {
                if (!pollutantName.equals("AQI")) { // Avoid duplicates
                    pollutants.add(new Pollutant(pollutantName, "Disconnected"));
                }
            }
        }

        return pollutants;
    }

    private void handleConnectionRequest() {
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_LONG).show();
            return;
        }

        if (!isLocationEnabled()) {
            Toast.makeText(this, "Please enable Location Services for BLE scanning", Toast.LENGTH_LONG).show();
            Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(locationIntent);
            return;
        }
        if (checkAndRequestPermissions()) {
            startScan();
        }
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private boolean checkAndRequestPermissions() {
        List<String> neededPermissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (!neededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, neededPermissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                Log.d(TAG, "All permissions granted. Starting scan.");
                handleConnectionRequest();
            } else {
                Log.w(TAG, "Not all permissions were granted.");
                Toast.makeText(this, "Permissions are required for BLE functionality.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void startScan() {
        if (MOCK_MODE) return;  // mock guard

        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth is not enabled.", Toast.LENGTH_SHORT).show();
            return;
        }

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner == null) {
            Log.e(TAG, "Failed to get BLE scanner, is Bluetooth enabled?");
            Toast.makeText(this, "Could not start scan. Is Bluetooth on?", Toast.LENGTH_LONG).show();
            return;
        }


        if (!scanning) {
            Log.d(TAG, "Starting BLE scan...");
            handler.postDelayed(() -> {
                if (scanning) {
                    scanning = false;
                    bluetoothLeScanner.stopScan(leScanCallback);
                    Log.d(TAG, "Scan stopped after timeout.");
                }
            }, SCAN_PERIOD);
            scanning = true;
            bluetoothLeScanner.startScan(leScanCallback);
        } else {
            scanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
            Log.d(TAG, "Scan stopped manually.");
        }
    }

    @SuppressLint("MissingPermission")
    private void stopScan() {
        if (MOCK_MODE) return;  // mock guard

        if (scanning && bluetoothLeScanner != null) { // also check if scanner is not null
            bluetoothLeScanner.stopScan(leScanCallback);
            scanning = false;
            Log.d(TAG, "Scan stopped.");
        }
    }

    private final ScanCallback leScanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            Log.d(TAG, "Device found: " + device.getName() + " with address: " + device.getAddress());
            if (DEVICE_ADDRESS.equalsIgnoreCase(device.getAddress())) {
                Log.i(TAG, "Target device found! Address: " + device.getAddress());
                if (bluetoothLeScanner != null) {
                    scanning=false;
                    bluetoothLeScanner.stopScan(leScanCallback);
                }

                // Also cancel the 10-second timeout handler to prevent it from firing later
                handler.removeCallbacksAndMessages(null);
                connectDevice(device);
            }
        }
    };

    @SuppressLint("MissingPermission")
    private void connectDevice(BluetoothDevice device) {
        Log.d(TAG, "Attempting to connect to device: " + device.getAddress());
        stopScan();
        bluetoothGatt = device.connectGatt(this, false, gattCallback);
    }

    @SuppressLint("MissingPermission")
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String deviceAddress = gatt.getDevice().getAddress();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "Successfully connected to " + deviceAddress);
                    gatt.requestMtu(517);//request biggest value
                    runOnUiThread(() -> {
                        connectButton.setText(R.string.status_connected);
                        connectButton.setEnabled(false);
                    });
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "Successfully disconnected from " + deviceAddress);
                    runOnUiThread(() -> {
                        for (Pollutant pollutant : pollutantList) {
                            pollutant.setValue("Disconnected");
                        }
                        pollutantAdapter.notifyDataSetChanged();
                        connectButton.setText(R.string.status_connect);
                        connectButton.setEnabled(true);
                    });
                }
            } else {
                Log.w(TAG, "Connection error with " + deviceAddress + ". Status: " + status);
                runOnUiThread(() -> {
                    for (Pollutant pollutant : pollutantList) {
                        pollutant.setValue("Disconnected");
                    }
                    pollutantAdapter.notifyDataSetChanged();
                    connectButton.setText(R.string.status_connect);
                    connectButton.setEnabled(true);
                });
            }
        }

        //method to request bigger mtu to nimBLE to receive 32bit packet
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "MTU changed successfully to: " + mtu);
                // Now that the MTU is set, we can discover services.
                gatt.discoverServices();
            } else {
                Log.w(TAG, "Failed to change MTU. Status: " + status);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered successfully.");
                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                if (service != null) {
                    Log.d(TAG, "Service found: " + service.getUuid());
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
                    if (characteristic != null) {
                        Log.d(TAG, "Characteristic found: " + characteristic.getUuid());
                        gatt.setCharacteristicNotification(characteristic, true);
                        UUID cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(cccdUuid);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        } else {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                        }

                    } else {
                        Log.e(TAG, "Characteristic not found: " + CHARACTERISTIC_UUID);
                    }
                } else {
                    Log.e(TAG, "Service not found. Status: " + status);
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Descriptor written successfully. Notifications enabled.");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
            handleCharacteristicChanged(value);
        }

        @Override
        @SuppressWarnings("deprecation")
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            handleCharacteristicChanged(characteristic.getValue());
        }

        private void handleCharacteristicChanged(byte[] data) {
            if (data != null && data.length == 36) { // 9 floats * 4 bytes/float
                Log.d(TAG, "Received correct 32-byte data packet. Parsing...");

                ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
                float co2 = buffer.getFloat();
                float tvoc = buffer.getFloat();
                float propane = buffer.getFloat();
                float co = buffer.getFloat();
                float smoke = buffer.getFloat();
                float alcohol = buffer.getFloat();
                float methane = buffer.getFloat();
                float h2 = buffer.getFloat();
                float aqi = buffer.getFloat();

                checkAirQualityLevels(co2, tvoc, co, smoke, propane, methane, alcohol, h2);
                calcSimpleIndex(co2, tvoc, co, smoke, propane, methane, alcohol, h2);


                long timestamp = System.currentTimeMillis() / 1000;
                boolean isInserted = myDb.insertData(timestamp, co2, tvoc, propane, co, smoke, alcohol, methane, h2, aqi);
                if (isInserted) {
                    Log.d(TAG, "Data Inserted into DB");
                } else {
                    Log.w(TAG, "Failed to insert data into DB");
                }

        // Save latest for Stats screen in real BLE mode too
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

                runOnUiThread(() -> {
                    for (Pollutant pollutant : pollutantList) {
                        switch (pollutant.getName()) {
                            case "CO2":
                                pollutant.setValue(String.format(Locale.US, "%.2f ppm", co2));
                                break;
                            case "TVOC":
                                pollutant.setValue(String.format(Locale.US, "%.2f ppb", tvoc));
                                break;
                            case "AQI":
                                pollutant.setValue(String.format(Locale.US, "%.2f", aqi));
                                break;
                            case "Propane":
                                pollutant.setValue(String.format(Locale.US, "%.2f ppb", propane));
                                break;
                            case "CO":
                                pollutant.setValue(String.format(Locale.US, "%.2f ppm", co));
                                break;
                            case "Smoke":
                                pollutant.setValue(String.format(Locale.US, "%.2f ppb", smoke));
                                break;
                            case "Alcohol":
                                pollutant.setValue(String.format(Locale.US, "%.2f ppb", alcohol));
                                break;
                            case "Methane":
                                pollutant.setValue(String.format(Locale.US, "%.2f ppb", methane));
                                break;
                            case "H2":
                                pollutant.setValue(String.format(Locale.US, "%.2f ppb", h2));
                                break;
                        }
                    }
                    pollutantAdapter.notifyDataSetChanged();
                });
            } else {
                Log.w(TAG, "Received malformed data packet. Length: " + (data != null ? data.length : 0));
                runOnUiThread(() -> {
                    for (Pollutant pollutant : pollutantList) {
                        pollutant.setValue("Error");
                    }
                    pollutantAdapter.notifyDataSetChanged();
                });
            }
        }
    };

    @SuppressLint("MissingPermission")
    @Override
    protected void onPause() {
        super.onPause();
        stopScan();
        // Keep BLE connection and mock generator running while navigating between activities
        // so long as the app remains in foreground/backstack. We only stop scanning.
        if (mockReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mockReceiver);
            mockReceiver = null;
        }
        // stop polling while paused
        handler.removeCallbacks(statsPoller);
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        // Stop mock service ONLY if the activity is finishing (app closing)
        if (MOCK_MODE && isFinishing()) {
            stopService(new Intent(this, MockDataService.class));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_home) {
            // Already on Home (MainActivity). Do nothing or refresh if you want.
            return true;
        } else if (id == R.id.action_statistics) {
            startActivity(new Intent(this, StatsActivity.class));
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
