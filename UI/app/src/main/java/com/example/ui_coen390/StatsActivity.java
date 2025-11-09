package com.example.ui_coen390;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.Random;

public class StatsActivity extends AppCompatActivity {

    private GraphView graphAqi, graphCo2, graphTvoc, graphPropane, graphCo, graphSmoke, graphAlcohol, graphMethane, graphH2;

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
        updateGraphs();
        //setupGraphs();

        // after a save see View->Tool Window-> Device explorer -> data/data/android.example.ui390/files ( or smth like that)
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

    private void setupGraphs() {
    }


    @Override
    protected void onResume() {
        super.onResume();
        updateGraphs();
    }

    private void updateGraphs() {

        DatabaseHelper myDb = new DatabaseHelper(this);
        ArrayList<SensorReading> readings = myDb.getLastNReadings(15); // last n readings

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


        for (int i = 0; i < readings.size(); i++) {
            SensorReading r = readings.get(i);
            seriesAqi.appendData(new DataPoint(i, r.getAqi()), true, readings.size());
            seriesCo2.appendData(new DataPoint(i, r.getCo2()), true, readings.size());
            seriesTvoc.appendData(new DataPoint(i, r.getTvoc()), true, readings.size());
            seriesPropane.appendData(new DataPoint(i, r.getPropane()), true, readings.size());
            seriesCo.appendData(new DataPoint(i, r.getCo()), true, readings.size());
            seriesSmoke.appendData(new DataPoint(i, r.getSmoke()), true, readings.size());
            seriesAlcohol.appendData(new DataPoint(i, r.getAlcohol()), true, readings.size());
            seriesMethane.appendData(new DataPoint(i, r.getMethane()), true, readings.size());
            seriesH2.appendData(new DataPoint(i, r.getH2()), true, readings.size());
        }

        // Clear old series
        graphAqi.removeAllSeries();
        graphCo2.removeAllSeries();
        graphTvoc.removeAllSeries();
        graphPropane.removeAllSeries();
        graphCo.removeAllSeries();
        graphSmoke.removeAllSeries();
        graphAlcohol.removeAllSeries();
        graphMethane.removeAllSeries();
        graphH2.removeAllSeries();

        // Add new series
        graphAqi.addSeries(seriesAqi);
        graphCo2.addSeries(seriesCo2);
        graphTvoc.addSeries(seriesTvoc);
        graphPropane.addSeries(seriesPropane);
        graphCo.addSeries(seriesCo);
        graphSmoke.addSeries(seriesSmoke);
        graphAlcohol.addSeries(seriesAlcohol);
        graphMethane.addSeries(seriesMethane);
        graphH2.addSeries(seriesH2);

        //autoscaling off
        graphAqi.getViewport().setXAxisBoundsManual(true);
        graphAqi.getViewport().setMaxX(readings.size() - 1);
        graphCo2.getViewport().setXAxisBoundsManual(true);
        graphCo2.getViewport().setMaxX(readings.size() - 1);
        graphTvoc.getViewport().setXAxisBoundsManual(true);
        graphTvoc.getViewport().setMaxX(readings.size() - 1);
        graphPropane.getViewport().setXAxisBoundsManual(true);
        graphPropane.getViewport().setMaxX(readings.size() - 1);
        graphCo.getViewport().setXAxisBoundsManual(true);
        graphCo.getViewport().setMaxX(readings.size() - 1);
        graphSmoke.getViewport().setXAxisBoundsManual(true);
        graphSmoke.getViewport().setMaxX(readings.size() - 1);
        graphAlcohol.getViewport().setXAxisBoundsManual(true);
        graphAlcohol.getViewport().setMaxX(readings.size() - 1);
        graphMethane.getViewport().setXAxisBoundsManual(true);
        graphMethane.getViewport().setMaxX(readings.size() - 1);
        graphH2.getViewport().setXAxisBoundsManual(true);
        graphH2.getViewport().setMaxX(readings.size() - 1);


        android.content.SharedPreferences prefs = getSharedPreferences("stats", MODE_PRIVATE);

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

        SensorReading latest = readings.get(readings.size()-1);
        ((android.widget.TextView) findViewById(R.id.textAqi)).setText(String.format("%.0f", latest.getAqi()));
        ((android.widget.TextView) findViewById(R.id.textCo2)).setText(String.format("%.0f ppm", latest.getCo2()));
        ((android.widget.TextView) findViewById(R.id.textTvoc)).setText(String.format("%.0f ppb", latest.getTvoc()));
        ((android.widget.TextView) findViewById(R.id.textPropane)).setText(String.format("%.0f ppb", latest.getPropane()));
        ((android.widget.TextView) findViewById(R.id.textCo)).setText(String.format("%.0f ppm", latest.getCo()));
        ((android.widget.TextView) findViewById(R.id.textSmoke)).setText(String.format("%.0f ppb", latest.getSmoke()));
        ((android.widget.TextView) findViewById(R.id.textAlcohol)).setText(String.format("%.0f ppb", latest.getAlcohol()));
        ((android.widget.TextView) findViewById(R.id.textMethane)).setText(String.format("%.0f ppb", latest.getMethane()));
        ((android.widget.TextView) findViewById(R.id.textH2)).setText(String.format("%.0f ppb", latest.getH2()));

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
