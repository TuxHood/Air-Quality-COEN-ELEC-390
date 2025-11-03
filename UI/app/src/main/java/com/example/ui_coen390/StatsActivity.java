package com.example.ui_coen390;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;


public class StatsActivity extends AppCompatActivity {

    private LineChart chart;
    private LineDataSet dataSetCo2, dataSetTvoc, dataSetAqi;
    private LineData lineData;
    private FloatingActionButton exportButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        // Up arrow (null-safe)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        chart = findViewById(R.id.airQualityChart);
        setupChart();
        // Initial populate (also done in onResume)
        updateFromPrefs();

        exportButton = findViewById(R.id.floatingActionButton);
        exportButton.setOnClickListener(v -> {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            boolean success = dbHelper.exportToJson(this);

            if (success) {
                Toast.makeText(this, "Data exported successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Export failed!", Toast.LENGTH_SHORT).show();
            }

            /*
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/json");
            File file = new File(getFilesDir(), "sensor_data_export.json");
            Uri uri = FileProvider.getUriForFile(this, "com.example.ui_coen390.fileprovider", file);
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            startActivity(Intent.createChooser(shareIntent, "Share sensor data"));
            */
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        updateFromPrefs();
    }

    private void updateFromPrefs() {

        android.content.SharedPreferences prefs = getSharedPreferences("stats", MODE_PRIVATE);
        float co2  = prefs.getFloat("co2",  0f);
        float tvoc = prefs.getFloat("tvoc", 0f);
        float aqi  = prefs.getFloat("aqi",  0f);

        ((android.widget.TextView) findViewById(R.id.textCo2))
                .setText(String.format(java.util.Locale.US, "%.0f ppm", co2));
        ((android.widget.TextView) findViewById(R.id.textTvoc))
                .setText(String.format(java.util.Locale.US, "%.0f ppb", tvoc));
        ((android.widget.TextView) findViewById(R.id.textAqi))
                .setText(String.format(java.util.Locale.US, "%.0f", aqi));

        DatabaseHelper myDb = new DatabaseHelper(this);
        ArrayList<SensorReading> readings = myDb.getLastNReadings(10); // last n readings

        dataSetCo2.clear();
        dataSetTvoc.clear();
        dataSetAqi.clear();

        int index = 0;
        for (SensorReading r : readings) {
            dataSetCo2.addEntry(new Entry(index, r.co2));
            dataSetTvoc.addEntry(new Entry(index, r.tvoc));
            dataSetAqi.addEntry(new Entry(index, r.aqi));
            index++;
        }

        lineData.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    private void setupChart() {
        dataSetCo2  = new LineDataSet(new java.util.ArrayList<>(), "CO₂ (ppm)");
        dataSetTvoc = new LineDataSet(new java.util.ArrayList<>(), "TVOC (ppb)");
        dataSetAqi  = new LineDataSet(new java.util.ArrayList<>(), "AQI");

        dataSetCo2.setColor(android.graphics.Color.RED);
        dataSetTvoc.setColor(android.graphics.Color.BLUE);
        dataSetAqi.setColor(android.graphics.Color.GREEN);

        dataSetCo2.setCircleRadius(3f);
        dataSetTvoc.setCircleRadius(3f);
        dataSetAqi.setCircleRadius(3f);

        lineData = new LineData();
        lineData.addDataSet(dataSetCo2);
        lineData.addDataSet(dataSetTvoc);
        lineData.addDataSet(dataSetAqi);

        chart.setData(lineData);
        chart.getDescription().setEnabled(false);
        chart.getXAxis().setDrawGridLines(false);
        chart.getAxisRight().setEnabled(false);
        chart.animateX(500);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();  // or getOnBackPressedDispatcher().onBackPressed();
            return true;
        } else if (id == R.id.action_home) {
            startActivity(new android.content.Intent(this, MainActivity.class)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP));
            return true;
        } else if (id == R.id.action_statistics) {
            // already here
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new android.content.Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
