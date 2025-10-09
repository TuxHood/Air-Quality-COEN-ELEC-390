package com.airquality.monitor;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String ESP32_NAME = "ESP32_AirQuality";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Thread workerThread;
    private volatile boolean stopWorker;

    private Button btnConnect;
    private Button btnDisconnect;
    private TextView tvConnectionStatus;
    private TextView tvCO2Level;
    private TextView tvTVOCLevel;
    private TextView tvSmokeLevel;
    private TextView tvAirQualityStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        checkPermissions();

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectToESP32();
            }
        });

        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnect();
            }
        });
    }

    private void initializeViews() {
        btnConnect = findViewById(R.id.btnConnect);
        btnDisconnect = findViewById(R.id.btnDisconnect);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        tvCO2Level = findViewById(R.id.tvCO2Level);
        tvTVOCLevel = findViewById(R.id.tvTVOCLevel);
        tvSmokeLevel = findViewById(R.id.tvSmokeLevel);
        tvAirQualityStatus = findViewById(R.id.tvAirQualityStatus);

        btnDisconnect.setEnabled(false);
    }

    private void checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(this, 
                new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                }, 
                REQUEST_BLUETOOTH_PERMISSIONS);
        }
    }

    private void connectToESP32() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            checkPermissions();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                BluetoothDevice esp32Device = null;
                
                try {
                    Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                    
                    for (BluetoothDevice device : pairedDevices) {
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            if (device.getName() != null && device.getName().equals(ESP32_NAME)) {
                                esp32Device = device;
                                break;
                            }
                        }
                    }

                    if (esp32Device == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "ESP32 device not found. Please pair it first.", Toast.LENGTH_LONG).show();
                            }
                        });
                        return;
                    }

                    bluetoothSocket = esp32Device.createRfcommSocketToServiceRecord(MY_UUID);
                    bluetoothSocket.connect();
                    inputStream = bluetoothSocket.getInputStream();
                    outputStream = bluetoothSocket.getOutputStream();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvConnectionStatus.setText("Connected to ESP32");
                            btnConnect.setEnabled(false);
                            btnDisconnect.setEnabled(true);
                        }
                    });

                    beginListenForData();

                } catch (IOException e) {
                    final String error = e.getMessage();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Connection failed: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void beginListenForData() {
        final Handler handler = new Handler(Looper.getMainLooper());
        final byte delimiter = 10; // newline character
        stopWorker = false;

        workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] readBuffer = new byte[1024];
                int readBufferPosition = 0;

                while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                    try {
                        int bytesAvailable = inputStream.available();
                        if (bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            inputStream.read(packetBytes);

                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                if (b == delimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "UTF-8");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            parseAndDisplayData(data);
                                        }
                                    });
                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (IOException ex) {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    private void parseAndDisplayData(String data) {
        // Expected format: "CO2:400,TVOC:50,SMOKE:100"
        try {
            String[] parts = data.split(",");
            for (String part : parts) {
                String[] keyValue = part.split(":");
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String value = keyValue[1].trim();

                    switch (key) {
                        case "CO2":
                            tvCO2Level.setText("CO2: " + value + " ppm");
                            break;
                        case "TVOC":
                            tvTVOCLevel.setText("TVOC: " + value + " ppb");
                            break;
                        case "SMOKE":
                            tvSmokeLevel.setText("Smoke: " + value);
                            break;
                    }
                }
            }
            updateAirQualityStatus();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateAirQualityStatus() {
        String co2Text = tvCO2Level.getText().toString();
        if (co2Text.contains(":")) {
            try {
                String co2Value = co2Text.split(":")[1].trim().split(" ")[0];
                int co2 = Integer.parseInt(co2Value);
                
                if (co2 < 1000) {
                    tvAirQualityStatus.setText("Air Quality: Good");
                } else if (co2 < 2000) {
                    tvAirQualityStatus.setText("Air Quality: Moderate");
                } else {
                    tvAirQualityStatus.setText("Air Quality: Poor");
                }
            } catch (Exception e) {
                tvAirQualityStatus.setText("Air Quality: Unknown");
            }
        }
    }

    private void disconnect() {
        try {
            stopWorker = true;
            if (outputStream != null) {
                outputStream.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }

            tvConnectionStatus.setText("Disconnected");
            btnConnect.setEnabled(true);
            btnDisconnect.setEnabled(false);
            tvCO2Level.setText("CO2: --");
            tvTVOCLevel.setText("TVOC: --");
            tvSmokeLevel.setText("Smoke: --");
            tvAirQualityStatus.setText("Air Quality: --");

        } catch (IOException e) {
            Toast.makeText(this, "Error disconnecting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnect();
    }
}
