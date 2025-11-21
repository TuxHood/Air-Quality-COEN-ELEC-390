package com.example.ui_coen390;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.HashSet;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    private LinearLayout checkboxContainer;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        checkboxContainer = findViewById(R.id.checkboxContainer);
        saveButton = findViewById(R.id.saveButton);

        loadSettings();

        saveButton.setOnClickListener(v -> {
            saveSettings();
            Toast.makeText(SettingsActivity.this, "Settings saved", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        Set<String> selectedPollutants = prefs.getStringSet("selectedPollutants", null);

        if (selectedPollutants == null) {
            // If no settings are saved, check all boxes by default
            for (int i = 0; i < checkboxContainer.getChildCount(); i++) {
                if (checkboxContainer.getChildAt(i) instanceof CheckBox) {
                    ((CheckBox) checkboxContainer.getChildAt(i)).setChecked(true);
                }
            }
        } else {
            for (int i = 0; i < checkboxContainer.getChildCount(); i++) {
                if (checkboxContainer.getChildAt(i) instanceof CheckBox) {
                    CheckBox checkBox = (CheckBox) checkboxContainer.getChildAt(i);
                    if (selectedPollutants.contains(checkBox.getText().toString())) {
                        checkBox.setChecked(true);
                    }
                }
            }
        }
    }

    private void saveSettings() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Set<String> selectedPollutants = new HashSet<>();
        selectedPollutants.add("AQI"); // Always add AQI

        for (int i = 0; i < checkboxContainer.getChildCount(); i++) {
            if (checkboxContainer.getChildAt(i) instanceof CheckBox) {
                CheckBox checkBox = (CheckBox) checkboxContainer.getChildAt(i);
                if (checkBox.isChecked()) {
                    selectedPollutants.add(checkBox.getText().toString());
                }
            }
        }

        editor.putStringSet("selectedPollutants", selectedPollutants);
        editor.apply();
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
            startActivity(new Intent(this, StatsActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        // Hide Settings and mode toggle when in SettingsActivity
        try {
            MenuItem settingsItem = menu.findItem(R.id.action_settings);
            if (settingsItem != null) settingsItem.setVisible(false);
            MenuItem toggle = menu.findItem(R.id.action_toggle_mode);
            if (toggle != null) toggle.setVisible(false);
        } catch (Exception ignored) {}
        return true;
    }
}
