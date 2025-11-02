package com.example.ui_coen390;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class StatsActivity extends AppCompatActivity {

    private LinearLayout graphsContainer;
    private DatabaseHelper myDb;
    private LayoutInflater inflater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        myDb = new DatabaseHelper(this);
        graphsContainer = findViewById(R.id.graphsContainer);
        inflater = LayoutInflater.from(this);

        setupGraphs();
    }

    private void setupGraphs() {
        List<String> pollutants = getPollutantsFromSettings();
        graphsContainer.removeAllViews(); // Clear existing graphs

        for (String pollutant : pollutants) {
            View graphCard = inflater.inflate(R.layout.graph_card_item, graphsContainer, false);
            TextView graphTitle = graphCard.findViewById(R.id.graphTitle);
            GraphView graphView = graphCard.findViewById(R.id.graphView);

            graphTitle.setText(pollutant + " Over Time");
            updateGraph(graphView, pollutant);

            graphsContainer.addView(graphCard);
        }
    }

    private List<String> getPollutantsFromSettings() {
        // TODO: Replace with actual implementation to get pollutants from settings
        List<String> pollutants = new ArrayList<>();
        pollutants.add("CO2");
        pollutants.add("AQI");
        pollutants.add("TVOC");
        return pollutants;
    }

    private void updateGraph(GraphView graph, String pollutant) {
        List<android.util.Pair<Long, Double>> data = myDb.getReadingsForPollutant(pollutant);

        DataPoint[] dataPoints = new DataPoint[data.size()];
        for (int i = 0; i < data.size(); i++) {
            dataPoints[i] = new DataPoint(new Date(data.get(i).first * 1000), data.get(i).second);
        }

        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(dataPoints);
        graph.removeAllSeries();
        graph.addSeries(series);

        // styling
        series.setColor(getResources().getColor(R.color.purple_500));
        series.setDrawDataPoints(true);
        series.setDataPointsRadius(10);
        series.setThickness(8);

        // X-axis date formatting
        graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(this, new SimpleDateFormat("HH:mm:ss", Locale.US)));
        graph.getGridLabelRenderer().setNumHorizontalLabels(3);

        // Set manual X bounds to have some padding
        if (!data.isEmpty()) {
            graph.getViewport().setMinX(data.get(0).first * 1000 - 10000);
            graph.getViewport().setMaxX(data.get(data.size() - 1).first * 1000 + 10000);
            graph.getViewport().setXAxisBoundsManual(true);
        }

        graph.getGridLabelRenderer().setHumanRounding(false);
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
            onBackPressed();
            return true;
        } else if (id == R.id.action_home) {
            startActivity(new android.content.Intent(this, MainActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP));
            return true;
        } else if (id == R.id.action_statistics) {
            // Already here
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new android.content.Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        myDb.close();
        super.onDestroy();
    }
}
