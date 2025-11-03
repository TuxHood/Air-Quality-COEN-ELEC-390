package com.example.ui_coen390;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.Random;

public class StatsActivity extends AppCompatActivity {

    private GraphView graphAqi, graphCo2, graphTvoc, graphPropane, graphCo, graphSmoke, graphAlcohol, graphMethane, graphH2;

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
        graphPropane = findViewById(R.id.graphPropane);
        graphCo = findViewById(R.id.graphCo);
        graphSmoke = findViewById(R.id.graphSmoke);
        graphAlcohol = findViewById(R.id.graphAlcohol);
        graphMethane = findViewById(R.id.graphMethane);
        graphH2 = findViewById(R.id.graphH2);

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

        // --- Propane Graph ---
        graphPropane.setTitle("Propane Over Time");

        // --- Co Graph ---
        graphCo.setTitle("CO Over Time");

        // --- Smoke Graph ---
        graphSmoke.setTitle("Smoke Over Time");

        // --- Alcohol Graph ---
        graphAlcohol.setTitle("Alcohol Over Time");

        // --- Methane Graph ---
        graphMethane.setTitle("Methane Over Time");

        // --- H2 Graph ---
        graphH2.setTitle("H2 Over Time");

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
        float propane = prefs.getFloat("propane", 0f);
        float co = prefs.getFloat("co", 0f);
        float smoke = prefs.getFloat("smoke", 0f);
        float alcohol = prefs.getFloat("alcohol", 0f);
        float methane = prefs.getFloat("methane", 0f);
        float h2 = prefs.getFloat("h2", 0f);
        float aqi  = prefs.getFloat("aqi",  0f);

        // Preferences toggles (adjust keys if your SettingsActivity uses different names)
        boolean showCo2 = prefs.getBoolean("show_co2", true);
        boolean showTvoc = prefs.getBoolean("show_tvoc", true);
        boolean showPropane = prefs.getBoolean("show_propane", true);
        boolean showCo = prefs.getBoolean("show_co", true);
        boolean showSmoke = prefs.getBoolean("show_smoke", true);
        boolean showAlcohol = prefs.getBoolean("show_alcohol", true);
        boolean showMethane = prefs.getBoolean("show_methane", true);
        boolean showH2 = prefs.getBoolean("show_h2", true);

        android.widget.TextView tCo2 = findViewById(R.id.textCo2);
        android.widget.TextView tTvoc = findViewById(R.id.textTvoc);
        android.widget.TextView tPropane = findViewById(R.id.textPropane);
        android.widget.TextView tCo = findViewById(R.id.textCo);
        android.widget.TextView tSmoke = findViewById(R.id.textSmoke);
        android.widget.TextView tAlcohol = findViewById(R.id.textAlcohol);
        android.widget.TextView tMethane = findViewById(R.id.textMethane);
        android.widget.TextView tH2 = findViewById(R.id.textH2);
        android.widget.TextView tAqi = findViewById(R.id.textAqi);

        tCo2.setText(String.format(java.util.Locale.US, "%.0f ppm", co2));
        tTvoc.setText(String.format(java.util.Locale.US, "%.0f ppb", tvoc));
        tPropane.setText(String.format(java.util.Locale.US, "%.0f ppb", propane));
        tCo.setText(String.format(java.util.Locale.US, "%.0f ppm", co));
        tSmoke.setText(String.format(java.util.Locale.US, "%.0f ppb", smoke));
        tAlcohol.setText(String.format(java.util.Locale.US, "%.0f ppb", alcohol));
        tMethane.setText(String.format(java.util.Locale.US, "%.0f ppb", methane));
        tH2.setText(String.format(java.util.Locale.US, "%.0f ppb", h2));
        tAqi.setText(String.format(java.util.Locale.US, "%.0f", aqi));

        graphCo2.setVisibility(showCo2 ? android.view.View.VISIBLE : android.view.View.INVISIBLE);
        tCo2.setVisibility(showCo2 ? android.view.View.VISIBLE : android.view.View.INVISIBLE);
        graphTvoc.setVisibility(showTvoc ? android.view.View.VISIBLE : android.view.View.INVISIBLE);
        tTvoc.setVisibility(showTvoc ? android.view.View.VISIBLE : android.view.View.INVISIBLE);
        graphPropane.setVisibility(showPropane ? android.view.View.VISIBLE : android.view.View.INVISIBLE);
        tPropane.setVisibility(showPropane ? android.view.View.VISIBLE : android.view.View.INVISIBLE);
        graphCo.setVisibility(showCo ? android.view.View.VISIBLE : android.view.View.INVISIBLE);
        tCo.setVisibility(showCo ? android.view.View.VISIBLE : android.view.View.INVISIBLE);
        graphSmoke.setVisibility(showSmoke ? android.view.View.VISIBLE : android.view.View.INVISIBLE);
        tSmoke.setVisibility(showSmoke ? android.view.View.VISIBLE : android.view.View.INVISIBLE);
        graphAlcohol.setVisibility(showAlcohol ? android.view.View.VISIBLE : android.view.View.INVISIBLE);
        tAlcohol.setVisibility(showAlcohol ? android.view.View.VISIBLE : android.view.View.INVISIBLE);
        graphMethane.setVisibility(showMethane ? android.view.View.VISIBLE : android.view.View.INVISIBLE);
        tMethane.setVisibility(showMethane ? android.view.View.VISIBLE : android.view.View.INVISIBLE);
        graphH2.setVisibility(showH2 ? android.view.View.VISIBLE : android.view.View.INVISIBLE);
        tH2.setVisibility(showH2 ? android.view.View.VISIBLE : android.view.View.INVISIBLE);

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
