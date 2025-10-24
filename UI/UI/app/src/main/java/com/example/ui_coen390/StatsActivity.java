package com.example.ui_coen390;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

public class StatsActivity extends AppCompatActivity {

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

        // Initial populate (also done in onResume)
        updateFromPrefs();
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
