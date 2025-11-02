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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.example.ui_coen390.databinding.ActivityMainBinding;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    private TextView co2TextView;
    private TextView tvocTextView;
    private TextView indexTextView;
    private Button connectButton;
    private DatabaseHelper myDb;

    private static final boolean MOCK_MODE = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        myDb = new DatabaseHelper(this);
        co2TextView = findViewById(R.id.CO2TextView);
        tvocTextView = findViewById(R.id.TVOCTextView);
        indexTextView = findViewById(R.id.indexTextView);
        connectButton = findViewById(R.id.homeButton);

        co2TextView.setText(R.string.status_disconnected);
        tvocTextView.setText(R.string.status_disconnected);
        indexTextView.setText(R.string.status_disconnected);

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
                Log.d(TAG, "Demo mode: skipping BLE, starting fake updates.");
            } else {
                handleConnectionRequest();  // real BLE path
            }
        });
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
                        co2TextView.setText(R.string.status_disconnected);
                        tvocTextView.setText(R.string.status_disconnected);
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
                        .putFloat("aqi", aqi)
                        .apply();

                runOnUiThread(() -> {
                    co2TextView.setText(String.format(Locale.US, "%.2f ppm", co2)); // Format the output nicely
                    tvocTextView.setText(String.format(Locale.US, "%.2f ppb", tvoc)); // Format the output nicely
                    indexTextView.setText(String.format(Locale.US, "%.2f ", aqi));
                });
            } else {
                Log.w(TAG, "Received malformed data packet. Length: " + (data != null ? data.length : 0));
                runOnUiThread(() -> {
                    co2TextView.setText(R.string.status_error);
                    tvocTextView.setText(R.string.status_error);
                    indexTextView.setText(R.string.status_error);
                });
            }
        }
    };

    @SuppressLint("MissingPermission")
    @Override
    protected void onPause() {
        super.onPause();
        stopScan();
        if (bluetoothGatt != null) {
            Log.d(TAG, "Disconnecting from GATT server.");
            bluetoothGatt.disconnect();
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothGatt != null) {
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
}
