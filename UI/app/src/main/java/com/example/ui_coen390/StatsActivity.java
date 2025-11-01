package com.example.ui_coen390;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.Random;

public class StatsActivity extends AppCompatActivity {

    private GraphView graphAqi, graphCo2, graphTvoc;

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

        graphAqi = findViewById(R.id.graphAqi);
        graphCo2 = findViewById(R.id.graphCo2);
        graphTvoc = findViewById(R.id.graphTvoc);

        // Initial populate (also done in onResume)
        updateFromPrefs();
        setupGraphs();
    }

    private void setupGraphs() {
        // --- AQI Graph ---
        LineGraphSeries<DataPoint> seriesAqi = new LineGraphSeries<>();
        // Generate some dummy data
        Random rnd = new Random();
        for (int i = 0; i < 10; i++) {
            seriesAqi.appendData(new DataPoint(i, 50 + rnd.nextInt(100)), true, 10);
        }
        graphAqi.addSeries(seriesAqi);
        graphAqi.setTitle("AQI Over Time");

        // --- CO2 Graph ---
        LineGraphSeries<DataPoint> seriesCo2 = new LineGraphSeries<>();
        for (int i = 0; i < 10; i++) {
            seriesCo2.appendData(new DataPoint(i, 400 + rnd.nextInt(200)), true, 10);
        }
        graphCo2.addSeries(seriesCo2);
        graphCo2.setTitle("CO2 Over Time");

        // --- TVOC Graph ---
        LineGraphSeries<DataPoint> seriesTvoc = new LineGraphSeries<>();
        for (int i = 0; i < 10; i++) {
            seriesTvoc.appendData(new DataPoint(i, 100 + rnd.nextInt(150)), true, 10);
        }
        graphTvoc.addSeries(seriesTvoc);
        graphTvoc.setTitle("TVOC Over Time");
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
