package com.example.ui_coen390;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class StatsActivity extends AppCompatActivity {

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        FloatingActionButton exportButton = findViewById(R.id.floatingActionButton);
        exportButton.setOnClickListener(v -> {
            exportDatabaseToCsv();
        });
    }

    // --- AQI calculation helpers (same approach as MainActivity) ---
    private float scaleToAQI(float value, float threshold) {
        if (threshold <= 0) return 0;
        float scaled = (value / threshold) * 100f;
        if (scaled > 500f) scaled = 500f;
        return scaled;
    }

    private float calcSimpleIndex(float co2, float tvoc, float co, float smoke, float propane, float methane, float alcohol, float h2) {
        float CO2_THRESHOLD       = 2000f;
        float TVOC_THRESHOLD      = 660f;
        float CO_THRESHOLD        = 9f;
        float PROPANE_THRESHOLD   = 2100f;
        float SMOKE_THRESHOLD     = 150f;
        float METHANE_THRESHOLD   = 1000f;
        float ALCOHOL_THRESHOLD   = 1000f;
        float H2_THRESHOLD        = 4100f;

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


    @Override
    protected void onResume() {
        super.onResume();
        mTimer = new Runnable() {
            @Override
            public void run() {
                updateGraphs();
                // update less frequently to match mock generator and reduce CPU usage
                mHandler.postDelayed(this, 5000);
            }
        };

        // Update immediately so UI isn't empty, then schedule next run aligned with mock timestamp
        updateGraphs();
        try {
            long intervalMs = 5000L;
            android.content.SharedPreferences stats = getSharedPreferences("stats", MODE_PRIVATE);
            long tsSec = stats.getLong("timestamp", 0L);
            if (tsSec <= 0L) {
                mHandler.postDelayed(mTimer, intervalMs);
            } else {
                long nowMs = System.currentTimeMillis();
                long lastMs = tsSec * 1000L;
                long elapsed = nowMs - lastMs;
                long delay = elapsed >= intervalMs ? 0L : (intervalMs - elapsed);
                mHandler.postDelayed(mTimer, delay);
            }
        } catch (Exception e) {
            mHandler.postDelayed(mTimer, 5000);
        }
    }

    @Override
    protected void onPause() {
        mHandler.removeCallbacks(mTimer);
        super.onPause();
    }

    @SuppressLint("SetTextI18n")
    private void updateGraphs() {
        Log.d("STATS", "updateGraphs called");

        // --- 1. Update Visibility based on Settings ---
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        Set<String> selectedPollutants = prefs.getStringSet("selectedPollutants", new HashSet<>());

        // Preferences toggles (adjust keys if your SettingsActivity uses different names)
        MaterialCardView cardAqi = findViewById(R.id.card_aqi);
        MaterialCardView cardCo2 = findViewById(R.id.card_co2);
        MaterialCardView cardTvoc = findViewById(R.id.card_tvoc);
        MaterialCardView cardPropane = findViewById(R.id.card_propane);
        MaterialCardView cardCo = findViewById(R.id.card_co);
        MaterialCardView cardSmoke = findViewById(R.id.card_smoke);
        MaterialCardView cardAlcohol = findViewById(R.id.card_alcohol);
        MaterialCardView cardMethane = findViewById(R.id.card_methane);
        MaterialCardView cardH2 = findViewById(R.id.card_h2);

        if (selectedPollutants.isEmpty()) {
            cardAqi.setVisibility(View.VISIBLE);
            cardCo2.setVisibility(View.VISIBLE);
            cardTvoc.setVisibility(View.VISIBLE);
            cardPropane.setVisibility(View.VISIBLE);
            cardCo.setVisibility(View.VISIBLE);
            cardSmoke.setVisibility(View.VISIBLE);
            cardAlcohol.setVisibility(View.VISIBLE);
            cardMethane.setVisibility(View.VISIBLE);
            cardH2.setVisibility(View.VISIBLE);
        } else {
            cardAqi.setVisibility(selectedPollutants.contains("AQI") ? View.VISIBLE : View.GONE);
            cardCo2.setVisibility(selectedPollutants.contains("CO2") ? View.VISIBLE : View.GONE);
            cardTvoc.setVisibility(selectedPollutants.contains("TVOC") ? View.VISIBLE : View.GONE);
            cardPropane.setVisibility(selectedPollutants.contains("Propane") ? View.VISIBLE : View.GONE);
            cardCo.setVisibility(selectedPollutants.contains("CO") ? View.VISIBLE : View.GONE);
            cardSmoke.setVisibility(selectedPollutants.contains("Smoke") ? View.VISIBLE : View.GONE);
            cardAlcohol.setVisibility(selectedPollutants.contains("Alcohol") ? View.VISIBLE : View.GONE);
            cardMethane.setVisibility(selectedPollutants.contains("Methane") ? View.VISIBLE : View.GONE);
            cardH2.setVisibility(selectedPollutants.contains("H2") ? View.VISIBLE : View.GONE);
        }

        // --- 2. Populate graphs with data ---
        try (DatabaseHelper myDb = new DatabaseHelper(this)) {
            ArrayList<SensorReading> readings = myDb.getAllReadings();

            if (readings.isEmpty()) return;

            // Prepare series for each sensor
            LineGraphSeries<DataPoint> seriesAqi = new LineGraphSeries<>();
            LineGraphSeries<DataPoint> seriesCo2 = new LineGraphSeries<>();
            LineGraphSeries<DataPoint> seriesTvoc = new LineGraphSeries<>();
            LineGraphSeries<DataPoint> seriesPropane = new LineGraphSeries<>();
            LineGraphSeries<DataPoint> seriesCo = new LineGraphSeries<>();
            LineGraphSeries<DataPoint> seriesSmoke = new LineGraphSeries<>();
            LineGraphSeries<DataPoint> seriesAlcohol = new LineGraphSeries<>();
            LineGraphSeries<DataPoint> seriesMethane = new LineGraphSeries<>();
            LineGraphSeries<DataPoint> seriesH2 = new LineGraphSeries<>();


            for (SensorReading r : readings) {
                // Prefer AQI stored in the database (computed once at insert time).
                // Only compute as a fallback if stored AQI is NaN (older rows before DB-side calc).
                float aqiToPlot = r.getAqi();
                if (Float.isNaN(aqiToPlot)) {
                    aqiToPlot = calcSimpleIndex(r.getCo2(), r.getTvoc(), r.getCo(), r.getSmoke(), r.getPropane(), r.getMethane(), r.getAlcohol(), r.getH2());
                }
                seriesAqi.appendData(new DataPoint(new Date(r.getTimestamp() * 1000), aqiToPlot), true, readings.size());
                seriesCo2.appendData(new DataPoint(new Date(r.getTimestamp() * 1000), r.getCo2()), true, readings.size());
                seriesTvoc.appendData(new DataPoint(new Date(r.getTimestamp() * 1000), r.getTvoc()), true, readings.size());
                seriesPropane.appendData(new DataPoint(new Date(r.getTimestamp() * 1000), r.getPropane()), true, readings.size());
                seriesCo.appendData(new DataPoint(new Date(r.getTimestamp() * 1000), r.getCo()), true, readings.size());
                seriesSmoke.appendData(new DataPoint(new Date(r.getTimestamp() * 1000), r.getSmoke()), true, readings.size());
                seriesAlcohol.appendData(new DataPoint(new Date(r.getTimestamp() * 1000), r.getAlcohol()), true, readings.size());
                seriesMethane.appendData(new DataPoint(new Date(r.getTimestamp() * 1000), r.getMethane()), true, readings.size());
                seriesH2.appendData(new DataPoint(new Date(r.getTimestamp() * 1000), r.getH2()), true, readings.size());
            }

            // Clear old series
            ((GraphView)findViewById(R.id.graphAqi)).removeAllSeries();
            ((GraphView)findViewById(R.id.graphCo2)).removeAllSeries();
            ((GraphView)findViewById(R.id.graphTvoc)).removeAllSeries();
            ((GraphView)findViewById(R.id.graphPropane)).removeAllSeries();
            ((GraphView)findViewById(R.id.graphCo)).removeAllSeries();
            ((GraphView)findViewById(R.id.graphSmoke)).removeAllSeries();
            ((GraphView)findViewById(R.id.graphAlcohol)).removeAllSeries();
            ((GraphView)findViewById(R.id.graphMethane)).removeAllSeries();
            ((GraphView)findViewById(R.id.graphH2)).removeAllSeries();

            // Add new series
            GraphView gAqi = findViewById(R.id.graphAqi);
            GraphView gCo2 = findViewById(R.id.graphCo2);
            GraphView gTvoc = findViewById(R.id.graphTvoc);
            GraphView gPropane = findViewById(R.id.graphPropane);
            GraphView gCo = findViewById(R.id.graphCo);
            GraphView gSmoke = findViewById(R.id.graphSmoke);
            GraphView gAlcohol = findViewById(R.id.graphAlcohol);
            GraphView gMethane = findViewById(R.id.graphMethane);
            GraphView gH2 = findViewById(R.id.graphH2);

            gAqi.addSeries(seriesAqi);
            gCo2.addSeries(seriesCo2);
            gTvoc.addSeries(seriesTvoc);
            gPropane.addSeries(seriesPropane);
            gCo.addSeries(seriesCo);
            gSmoke.addSeries(seriesSmoke);
            gAlcohol.addSeries(seriesAlcohol);
            gMethane.addSeries(seriesMethane);
            gH2.addSeries(seriesH2);

            // Configure viewports: show a recent time window (makes charts readable)
            long latestMs = readings.get(readings.size() - 1).getTimestamp() * 1000L;
            long firstMs = readings.get(0).getTimestamp() * 1000L;
            // Default window: 10 minutes
            long windowMs = 10 * 60 * 1000L;
            // If data span is smaller than default, show full span; if much larger, still show recent window
            long span = latestMs - firstMs;
            long minX;
            if (span <= 0) {
                minX = Math.max(0L, latestMs - windowMs);
            } else if (span <= windowMs) {
                minX = firstMs;
            } else {
                minX = latestMs - windowMs;
            }

            GraphView[] graphs = new GraphView[]{gAqi, gCo2, gTvoc, gPropane, gCo, gSmoke, gAlcohol, gMethane, gH2};
            for (GraphView g : graphs) {
                // set a manual X axis window so we can focus on recent data
                g.getViewport().setXAxisBoundsManual(true);
                g.getViewport().setMinX(minX);
                g.getViewport().setMaxX(latestMs);

                // Let Y auto-scale for readability
                g.getViewport().setYAxisBoundsManual(false);

                // Allow users to pinch/scroll if they want to inspect older data
                g.getViewport().setScalable(true);
                g.getViewport().setScrollable(true);
            }

            //X-axis date formatting
            ((GraphView)findViewById(R.id.graphAqi)).getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(((GraphView)findViewById(R.id.graphAqi)).getViewport()));
            ((GraphView)findViewById(R.id.graphCo2)).getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(((GraphView)findViewById(R.id.graphCo2)).getViewport()));
            ((GraphView)findViewById(R.id.graphTvoc)).getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(((GraphView)findViewById(R.id.graphTvoc)).getViewport()));
            ((GraphView)findViewById(R.id.graphPropane)).getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(((GraphView)findViewById(R.id.graphPropane)).getViewport()));
            ((GraphView)findViewById(R.id.graphCo)).getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(((GraphView)findViewById(R.id.graphCo)).getViewport()));
            ((GraphView)findViewById(R.id.graphSmoke)).getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(((GraphView)findViewById(R.id.graphSmoke)).getViewport()));
            ((GraphView)findViewById(R.id.graphAlcohol)).getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(((GraphView)findViewById(R.id.graphAlcohol)).getViewport()));
            ((GraphView)findViewById(R.id.graphMethane)).getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(((GraphView)findViewById(R.id.graphMethane)).getViewport()));
            ((GraphView)findViewById(R.id.graphH2)).getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(((GraphView)findViewById(R.id.graphH2)).getViewport()));


            SensorReading latest = readings.get(readings.size() - 1);
            float latestAqi = latest.getAqi();
            if (Float.isNaN(latestAqi)) {
                latestAqi = calcSimpleIndex(latest.getCo2(), latest.getTvoc(), latest.getCo(), latest.getSmoke(), latest.getPropane(), latest.getMethane(), latest.getAlcohol(), latest.getH2());
            }
            ((android.widget.TextView) findViewById(R.id.textAqi)).setText(String.format("%.0f", latestAqi));
            ((android.widget.TextView) findViewById(R.id.textCo2)).setText(String.format("%.0f ppm", latest.getCo2()));
            ((android.widget.TextView) findViewById(R.id.textTvoc)).setText(String.format("%.0f ppb", latest.getTvoc()));
            ((android.widget.TextView) findViewById(R.id.textPropane)).setText(String.format("%.0f ppb", latest.getPropane()));
            ((android.widget.TextView) findViewById(R.id.textCo)).setText(String.format("%.0f ppm", latest.getCo()));
            ((android.widget.TextView) findViewById(R.id.textSmoke)).setText(String.format("%.0f ppb", latest.getSmoke()));
            ((android.widget.TextView) findViewById(R.id.textAlcohol)).setText(String.format("%.0f ppb", latest.getAlcohol()));
            ((android.widget.TextView) findViewById(R.id.textMethane)).setText(String.format("%.0f ppb", latest.getMethane()));
            ((android.widget.TextView) findViewById(R.id.textH2)).setText(String.format("%.0f ppb", latest.getH2()));

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        // Hide Statistics menu item and the mode toggle when in StatsActivity
        try {
            MenuItem statsItem = menu.findItem(R.id.action_statistics);
            if (statsItem != null) statsItem.setVisible(false);
            MenuItem toggle = menu.findItem(R.id.action_toggle_mode);
            if (toggle != null) toggle.setVisible(false);
        } catch (Exception ignored) {}
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_home) {
            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            return true;
        } else if (id == R.id.action_statistics) {
            // already here
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void exportDatabaseToCsv() {
        String fileName = "SensorData_" + System.currentTimeMillis() + ".csv";

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);

        if (uri != null) {
            try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                DatabaseHelper dbHelper = new DatabaseHelper(this);
                ArrayList<SensorReading> readings = dbHelper.getAllReadings();

                // Header
                outputStream.write("Timestamp,CO2,TVOC,Propane,CO,Smoke,Alcohol,Methane,H2,AQI\n".getBytes());

                for (SensorReading reading : readings) {
                    String line = reading.getTimestamp() + "," +
                            reading.getCo2() + "," +
                            reading.getTvoc() + "," +
                            reading.getPropane() + "," +
                            reading.getCo() + "," +
                            reading.getSmoke() + "," +
                            reading.getAlcohol() + "," +
                            reading.getMethane() + "," +
                            reading.getH2() + "," +
                            reading.getAqi() + "\n";
                    outputStream.write(line.getBytes());
                }

                Toast.makeText(this, "Exported to Downloads", Toast.LENGTH_LONG).show();

            } catch (IOException e) {
                Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Failed to create new file for export", Toast.LENGTH_LONG).show();
        }
    }
}
