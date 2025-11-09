package com.example.ui_coen390;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

public class SettingsActivity extends AppCompatActivity {

    private CheckBox CO2checkBox;
    private CheckBox TVOCcheckBox;
    private CheckBox propanecheckBox;
    private CheckBox COcheckBox;
    private CheckBox smokecheckBox;
    private CheckBox alcoholcheckBox;
    private CheckBox methanecheckBox;
    private CheckBox H2checkBox;
    private Button saveButton;

    private SharedPreferences prefsMain;
    private SharedPreferences prefsStats;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefsMain = getSharedPreferences("sensor_prefs", MODE_PRIVATE);
        prefsStats = getSharedPreferences("stats", MODE_PRIVATE);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        // Optional up arrow:
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        CO2checkBox = findViewById(R.id.CO2checkBox);
        TVOCcheckBox = findViewById(R.id.TVOCcheckBox);
        propanecheckBox = findViewById(R.id.propanecheckBox);
        COcheckBox = findViewById(R.id.COcheckBox);
        smokecheckBox = findViewById(R.id.smokecheckBox);
        alcoholcheckBox = findViewById(R.id.alcoholcheckBox);
        methanecheckBox = findViewById(R.id.methanecheckBox);
        H2checkBox = findViewById(R.id.H2checkBox);
        saveButton = findViewById(R.id.saveButton);

        CO2checkBox.setChecked(prefsMain.getBoolean("show_co2", true));
        TVOCcheckBox.setChecked(prefsMain.getBoolean("show_tvoc", true));
        propanecheckBox.setChecked(prefsMain.getBoolean("show_propane", true));
        COcheckBox.setChecked(prefsMain.getBoolean("show_co", true));
        smokecheckBox.setChecked(prefsMain.getBoolean("show_smoke", true));
        alcoholcheckBox.setChecked(prefsMain.getBoolean("show_alcohol", true));
        methanecheckBox.setChecked(prefsMain.getBoolean("show_methane", true));
        H2checkBox.setChecked(prefsMain.getBoolean("show_h2", true));

        CO2checkBox.setChecked(prefsStats.getBoolean("show_co2", true));
        TVOCcheckBox.setChecked(prefsStats.getBoolean("show_tvoc", true));
        propanecheckBox.setChecked(prefsStats.getBoolean("show_propane", true));
        COcheckBox.setChecked(prefsStats.getBoolean("show_co", true));
        smokecheckBox.setChecked(prefsStats.getBoolean("show_smoke", true));
        alcoholcheckBox.setChecked(prefsStats.getBoolean("show_alcohol", true));
        methanecheckBox.setChecked(prefsStats.getBoolean("show_methane", true));
        H2checkBox.setChecked(prefsStats.getBoolean("show_h2", true));

        saveButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editorMain = prefsMain.edit();
                editorMain.putBoolean("show_co2", CO2checkBox.isChecked());
                editorMain.putBoolean("show_tvoc", TVOCcheckBox.isChecked());
                editorMain.putBoolean("show_propane", propanecheckBox.isChecked());
                editorMain.putBoolean("show_co", COcheckBox.isChecked());
                editorMain.putBoolean("show_smoke", smokecheckBox.isChecked());
                editorMain.putBoolean("show_alcohol", alcoholcheckBox.isChecked());
                editorMain.putBoolean("show_methane", methanecheckBox.isChecked());
                editorMain.putBoolean("show_h2", H2checkBox.isChecked());
                editorMain.apply();

                SharedPreferences.Editor editorStats = prefsStats.edit();
                editorStats.putBoolean("show_co2", CO2checkBox.isChecked());
                editorStats.putBoolean("show_tvoc", TVOCcheckBox.isChecked());
                editorStats.putBoolean("show_propane", propanecheckBox.isChecked());
                editorStats.putBoolean("show_co", COcheckBox.isChecked());
                editorStats.putBoolean("show_smoke", smokecheckBox.isChecked());
                editorStats.putBoolean("show_alcohol", alcoholcheckBox.isChecked());
                editorStats.putBoolean("show_methane", methanecheckBox.isChecked());
                editorStats.putBoolean("show_h2", H2checkBox.isChecked());
                editorStats.apply();

                Toast.makeText(SettingsActivity.this, "Settings saved", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) { // back arrow
            onBackPressed();
            return true;
        } else if (id == R.id.action_home) {
            startActivity(new android.content.Intent(this, MainActivity.class)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP));
            return true;
        } else if (id == R.id.action_statistics) {
            startActivity(new android.content.Intent(this, StatsActivity.class));
            return true;
        } else if (id == R.id.action_settings) {
            // already here
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
