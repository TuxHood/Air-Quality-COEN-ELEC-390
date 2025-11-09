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
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.example.ui_coen390.databinding.ActivityMainBinding;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.FileWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.json.JSONArray;

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

    private TextView co2LabelTextView;
    private TextView tvocLabelTextView;
    private TextView propaneLabelTextView;
    private TextView coLabelTextView;
    private TextView smokeLabelTextView;
    private TextView alcoholLabelTextView;
    private TextView methaneLabelTextView;
    private TextView h2LabelTextView;
    private TextView co2TextView;
    private TextView tvocTextView;
    private TextView propaneTextView;
    private TextView coTextView;
    private TextView smokeTextView;
    private TextView alcoholTextView;
    private TextView methaneTextView;
    private TextView h2TextView;
    private TextView indexTextView;

    private Button connectButton;
    private DatabaseHelper myDb;

    //****** Mock/demo mode: run the app without any Bluetooth hardware **************************************************************************
    private static final boolean MOCK_MODE = true; //to disable mock mode and use real hardware, comment this line
    //private static final boolean MOCK_MODE = false; //to disable mock mode and use real hardware, uncomment this line

    private final Runnable mockUpdater = new Runnable() {
        @Override public void run() {
            // Generate simple fake values
            float co2  = 350 + (float)(Math.random() * 300);  // ppm
            float tvoc =  2  + (float)(Math.random() * 40);   // ppb
            float propane = 0.5f + (float)(Math.random() * 10.0f);  // 0.5 - 10.5 ppm
            float co   =   0 + (float)(Math.random() * 50);    // ppm (e.g., 0–50)
            float smoke   = 0f   + (float)(Math.random() * 40.0f);  // 0 - 40 ppm
            float alcohol = 0f   + (float)(Math.random() * 8.0f);   // 0 - 8 ppm
            float methane = 0 + (float)(Math.random() * 5);    // ppm (e.g., 0–5)
            float h2      = 0f   + (float)(Math.random() * 50.0f);  // 0 - 50 ppm
            float aqi = calcSimpleIndex(co2, tvoc, co, methane, propane, smoke, alcohol, h2);

            runOnUiThread(() -> {
                co2TextView.setText(String.format(Locale.US, "%.0f ppm", co2));
                tvocTextView.setText(String.format(Locale.US, "%.0f ppb", tvoc));
                propaneTextView.setText(String.format(Locale.US, "%.0f ppm", propane));
                coTextView.setText(String.format(Locale.US, "%.0f ppm", co));
                smokeTextView.setText(String.format(Locale.US, "%.0f ppm", smoke));
                alcoholTextView.setText(String.format(Locale.US, "%.0f ppm", alcohol));
                methaneTextView.setText(String.format(Locale.US, "%.0f ppm", methane));
                h2TextView.setText(String.format(Locale.US, "%.0f ppm", h2));
                indexTextView.setText(String.format(Locale.US, "%.0f ppb", aqi));
            });

            // Save latest values for the Stats screen (in-scope variables)
            getSharedPreferences("stats", MODE_PRIVATE).edit()
                    .putFloat("co2", co2)
                    .putFloat("tvoc", tvoc)
                    .putFloat("propane", propane)
                    .putFloat("co", co)
                    .putFloat("smoke", smoke)
                    .putFloat("alcohol", alcohol)
                    .putFloat("methane", methane)
                    .putFloat("h2", h2)
                    .putFloat("aqi", calcSimpleIndex(co2, tvoc, co, methane, propane, smoke, alcohol, h2))
                    .apply();

            saveReading(co2,tvoc, aqi);

            handler.postDelayed(this, 2000); // update every 2 seconds
        }
    };

    private void startMockMode() {
        runOnUiThread(() -> {
            co2TextView.setText("Connecting...");
            tvocTextView.setText("Connecting...");
            propaneTextView.setText("Connecting...");
            coTextView.setText("Connecting...");
            smokeTextView.setText("Connecting...");
            alcoholTextView.setText("Connecting...");
            methaneTextView.setText("Connecting...");
            h2TextView.setText("Connecting...");
            indexTextView.setText("Connecting...");
            connectButton.setEnabled(false);
        });
        handler.postDelayed(() -> {
            connectButton.setEnabled(true);
            connectButton.setText("Demo Running");
            mockUpdater.run(); // start periodic fake data
        }, 800);
    }

    private float calcSimpleIndex(float co2, float tvoc, float co, float methane, float propane, float smoke, float alcohol, float h2) {
        // Normalize each sensor to a 0-500 "score" and return the worst (max) score.
        float co2Score      = Math.min(500f, co2 / 2f);            // e.g., 1000 ppm -> 500
        float tvocScore     = Math.min(500f, tvoc * 5f);           // e.g., 100 ppb -> 500
        float coScore       = Math.min(500f, co * 10f);            // e.g., 50 ppm -> 500
        float methaneScore  = Math.min(500f, methane * 100f);     // e.g., 5 ppm -> 500
        float propaneScore  = Math.min(500f, propane * 50f);       // maps ~10 ppm -> 500
        float smokeScore    = Math.min(500f, smoke * 12.5f);       // maps ~40 ppm -> 500
        float alcoholScore  = Math.min(500f, alcohol * 62.5f);     // maps ~8 ppm -> 500
        float h2Score       = Math.min(500f, h2 * 10f);            // maps ~50 ppm -> 500

        float max1 = Math.max(co2Score, tvocScore);
        float max2 = Math.max(coScore, methaneScore);
        float max3 = Math.max(propaneScore, smokeScore);
        float max4 = Math.max(alcoholScore, h2Score);

        return Math.max(Math.max(max1, max2), Math.max(max3, max4));
    }
    //*******************************************************************************************************************************************

    private void updateFromPrefs() {
        android.content.SharedPreferences prefs = getSharedPreferences("sensor_prefs", MODE_PRIVATE);

        boolean showCO2 = prefs.getBoolean("show_co2", true);
        boolean showTVOC = prefs.getBoolean("show_tvoc", true);
        boolean showPropane = prefs.getBoolean("show_propane", true);
        boolean showCO = prefs.getBoolean("show_co", true);
        boolean showSmoke = prefs.getBoolean("show_smoke", true);
        boolean showAlcohol = prefs.getBoolean("show_alcohol", true);
        boolean showMethane = prefs.getBoolean("show_methane", true);
        boolean showH2 = prefs.getBoolean("show_h2", true);

        // Map preferences to your UI elements
        co2TextView.setVisibility(showCO2 ? View.VISIBLE : View.INVISIBLE);
        co2LabelTextView.setVisibility(showCO2 ? View.VISIBLE : View.INVISIBLE);
        tvocTextView.setVisibility(showTVOC ? View.VISIBLE : View.INVISIBLE);
        tvocLabelTextView.setVisibility(showTVOC ? View.VISIBLE : View.INVISIBLE);
        propaneTextView.setVisibility(showPropane ? View.VISIBLE : View.INVISIBLE);
        propaneLabelTextView.setVisibility(showPropane ? View.VISIBLE : View.INVISIBLE);
        coTextView.setVisibility(showCO ? View.VISIBLE : View.INVISIBLE);
        coLabelTextView.setVisibility(showCO ? View.VISIBLE : View.INVISIBLE);
        smokeTextView.setVisibility(showSmoke ? View.VISIBLE : View.INVISIBLE);
        smokeLabelTextView.setVisibility(showSmoke ? View.VISIBLE : View.INVISIBLE);
        alcoholTextView.setVisibility(showAlcohol ? View.VISIBLE : View.INVISIBLE);
        alcoholLabelTextView.setVisibility(showAlcohol ? View.VISIBLE : View.INVISIBLE);
        methaneTextView.setVisibility(showMethane ? View.VISIBLE : View.INVISIBLE);
        methaneLabelTextView.setVisibility(showMethane ? View.VISIBLE : View.INVISIBLE);
        h2TextView.setVisibility(showH2 ? View.VISIBLE : View.INVISIBLE);
        h2LabelTextView.setVisibility(showH2 ? View.VISIBLE : View.INVISIBLE);

    }

    @Override
    protected void onResume() {
        super.onResume();
        updateFromPrefs();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        myDb = new DatabaseHelper(this);
        co2TextView = findViewById(R.id.CO2TextView);
        co2LabelTextView = findViewById(R.id.co2LabelTextView);
        tvocTextView = findViewById(R.id.TVOCTextView);
        tvocLabelTextView = findViewById(R.id.tvocLabelTextView);
        propaneTextView = findViewById(R.id.PropaneTextView);
        propaneLabelTextView = findViewById(R.id.propaneLabelTextView);
        coTextView = findViewById(R.id.COTextView);
        coLabelTextView = findViewById(R.id.coLabelTextView);
        smokeTextView = findViewById(R.id.SmokeTextView);
        smokeLabelTextView = findViewById(R.id.smokeLabelTextView);
        alcoholTextView = findViewById(R.id.AlcoholTextView);
        alcoholLabelTextView = findViewById(R.id.alcoholLabelTextView);
        methaneTextView = findViewById(R.id.MethaneTextView);
        methaneLabelTextView = findViewById(R.id.methaneLabelTextView);
        h2TextView = findViewById(R.id.H2TextView);
        h2LabelTextView = findViewById(R.id.h2LabelTextView);

        indexTextView = findViewById(R.id.indexTextView);
        updateFromPrefs();

        connectButton = findViewById(R.id.homeButton); // Using homeButton as connect button

        co2TextView.setText(R.string.status_disconnected);
        tvocTextView.setText(R.string.status_disconnected);
        propaneTextView.setText(R.string.status_disconnected);
        coTextView.setText(R.string.status_disconnected);
        smokeTextView.setText(R.string.status_disconnected);
        alcoholTextView.setText(R.string.status_disconnected);
        methaneTextView.setText(R.string.status_disconnected);
        h2TextView.setText(R.string.status_disconnected);
        indexTextView.setText(R.string.status_disconnected);

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (MOCK_MODE) {
            connectButton.setOnClickListener(v -> {
                Log.d(TAG, "Demo mode: skipping BLE, starting fake updates.");
                startMockMode();
            });
        } else {
            connectButton.setOnClickListener(v -> {
                Log.d(TAG, "Connect button pressed.");
                handleConnectionRequest();  // real BLE path
            });
        }
    }

    private void handleConnectionRequest() {
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
                startScan();
            } else {
                Log.w(TAG, "Not all permissions were granted.");
                Toast.makeText(this, "Permissions are required for BLE functionality.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startScan() {
        if (MOCK_MODE) return;  // mock guard

        if (!checkAndRequestPermissions()) {
            Log.w(TAG, "startScan called without all necessary permissions.");
            return;
        }

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (!scanning) {
            Log.d(TAG, "Starting BLE scan...");
            handler.postDelayed(() -> {
                if (scanning) {
                    scanning = false;
                    //noinspection MissingPermission
                    bluetoothLeScanner.stopScan(leScanCallback);
                    Log.d(TAG, "Scan stopped after timeout.");
                }
            }, SCAN_PERIOD);
            scanning = true;
            //noinspection MissingPermission
            bluetoothLeScanner.startScan(leScanCallback);
        } else {
            scanning = false;
            //noinspection MissingPermission
            bluetoothLeScanner.stopScan(leScanCallback);
            Log.d(TAG, "Scan stopped manually.");
        }
    }

    private void stopScan() {
        if (MOCK_MODE) return;  // mock guard

        if (scanning) {
            //noinspection MissingPermission
            bluetoothLeScanner.stopScan(leScanCallback);
            scanning = false;
            Log.d(TAG, "Scan stopped.");
        }
    }

    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            //noinspection MissingPermission
            Log.d(TAG, "Device found: " + device.getName() + " with address: " + device.getAddress());
            //noinspection MissingPermission
            if (DEVICE_ADDRESS.equalsIgnoreCase(device.getAddress())) {
                Log.i(TAG, "Target device found! Address: " + device.getAddress());
                scanning=false;
                bluetoothLeScanner.stopScan(leScanCallback);

                // Also cancel the 10-second timeout handler to prevent it from firing later
                handler.removeCallbacksAndMessages(null);
                connectDevice(device);
            }
        }
    };

    private void connectDevice(BluetoothDevice device) {
        Log.d(TAG, "Attempting to connect to device: " + device.getAddress());
        stopScan();
        //noinspection MissingPermission
        bluetoothGatt = device.connectGatt(this, false, gattCallback);
    }

    @SuppressLint("MissingPermission")
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            //noinspection MissingPermission
            String deviceAddress = gatt.getDevice().getAddress();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "Successfully connected to " + deviceAddress);
                    gatt.requestMtu(517);//request biggest value
                    runOnUiThread(() -> {
                        connectButton.setText(R.string.status_connected);
                        connectButton.setEnabled(false);
                    });
                    //noinspection MissingPermission
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "Successfully disconnected from " + deviceAddress);
                    runOnUiThread(() -> {
                        co2TextView.setText(R.string.status_disconnected);
                        tvocTextView.setText(R.string.status_disconnected);
                        propaneTextView.setText(R.string.status_disconnected);
                        coTextView.setText(R.string.status_disconnected);
                        smokeTextView.setText(R.string.status_disconnected);
                        alcoholTextView.setText(R.string.status_disconnected);
                        methaneTextView.setText(R.string.status_disconnected);
                        h2TextView.setText(R.string.status_disconnected);
                        indexTextView.setText(R.string.status_disconnected);
                        connectButton.setText(R.string.status_connect);
                        connectButton.setEnabled(true);
                    });
                }
            } else {
                Log.w(TAG, "Connection error with " + deviceAddress + ". Status: " + status);
                runOnUiThread(() -> {
                    co2TextView.setText(R.string.status_disconnected);
                    tvocTextView.setText(R.string.status_disconnected);
                    propaneTextView.setText(R.string.status_disconnected);
                    coTextView.setText(R.string.status_disconnected);
                    smokeTextView.setText(R.string.status_disconnected);
                    alcoholTextView.setText(R.string.status_disconnected);
                    methaneTextView.setText(R.string.status_disconnected);
                    h2TextView.setText(R.string.status_disconnected);
                    indexTextView.setText(R.string.status_disconnected);
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
                        //noinspection MissingPermission
                        gatt.setCharacteristicNotification(characteristic, true);
                        UUID cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(cccdUuid);
                        //noinspection deprecation, MissingPermission
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        //noinspection deprecation, MissingPermission
                        gatt.writeDescriptor(descriptor);
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
            //noinspection deprecation
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

                long timestamp = System.currentTimeMillis() / 1000;
                boolean isInserted = myDb.insertData(timestamp, co2, tvoc, propane, co, smoke, alcohol, methane, h2, aqi);
                if (isInserted) {
                    Log.d(TAG, "Data Inserted into DB");
                } else {
                    Log.w(TAG, "Failed to insert data into DB");
                }

                // Save latest for Stats screen in real BLE mode too
                getSharedPreferences("stats", MODE_PRIVATE).edit()
                        .putFloat("co2", co2)
                        .putFloat("tvoc", tvoc)
                        .putFloat("aqi", calcSimpleIndex(co2, tvoc, co, methane, propane, smoke, alcohol, h2))
                        .apply();


                runOnUiThread(() -> {
                    co2TextView.setText(String.format(Locale.US, "%.2f ppm", co2));
                    tvocTextView.setText(String.format(Locale.US, "%.2f ppb", tvoc));
                    propaneTextView.setText(String.format(Locale.US, "%.2f ppm", propane));
                    coTextView.setText(String.format(Locale.US, "%.2f ppm", co));
                    smokeTextView.setText(String.format(Locale.US, "%.2f ppm", smoke));
                    alcoholTextView.setText(String.format(Locale.US, "%.2f ppm", alcohol));
                    methaneTextView.setText(String.format(Locale.US, "%.2f ppm", methane));
                    h2TextView.setText(String.format(Locale.US, "%.2f ppm", h2));
                    indexTextView.setText(String.format(Locale.US, "%.2f", aqi));
                });
            } else {
                Log.w(TAG, "Received malformed data packet. Length: " + (data != null ? data.length : 0));
                runOnUiThread(() -> {
                    co2TextView.setText(R.string.status_error);
                    tvocTextView.setText(R.string.status_error);
                    propaneTextView.setText(R.string.status_error);
                    coTextView.setText(R.string.status_error);
                    smokeTextView.setText(R.string.status_error);
                    alcoholTextView.setText(R.string.status_error);
                    methaneTextView.setText(R.string.status_error);
                    h2TextView.setText(R.string.status_error);
                    indexTextView.setText(R.string.status_error);
                });
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        stopScan();
        if (bluetoothGatt != null) {
            //noinspection MissingPermission
            Log.d(TAG, "Disconnecting from GATT server.");
            //noinspection MissingPermission
            bluetoothGatt.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothGatt != null) {
            //noinspection MissingPermission
            bluetoothGatt.close();
            bluetoothGatt = null;
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
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_statistics) {
            startActivity(new Intent(this, StatsActivity.class));  //fix: go to Stats
            return true;
        } else if (id == R.id.action_home) {
            // Already on Home (MainActivity). Do nothing or refresh if you want.
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void saveReading(float co2, float tvoc, float aqi) {

        long timestamp = System.currentTimeMillis() / 1000; // Unix timestamp in seconds

        // Insert into SQLite database
        myDb.insertData(timestamp, co2, tvoc, 0, 0, 0, 0, 0, 0, aqi);

    }
}
