package com.example.ui_coen390;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

public class SettingsActivity extends AppCompatActivity {

    public static final String KEY_SHOW_CO2 = "show_co2";
    public static final String KEY_SHOW_TVOC = "show_tvoc";
    public static final String KEY_SHOW_PROPANE = "show_propane";
    public static final String KEY_SHOW_CO = "show_co";
    public static final String KEY_SHOW_SMOKE = "show_smoke";
    public static final String KEY_SHOW_ALCOHOL = "show_alcohol";
    public static final String KEY_SHOW_METHANE = "show_methane";
    public static final String KEY_SHOW_H2 = "show_h2";

    private SharedPreferences sharedPreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Optional up arrow:
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setupCheckbox(R.id.checkbox_CO2, KEY_SHOW_CO2);
        setupCheckbox(R.id.checkbox_TVOC, KEY_SHOW_TVOC);
        setupCheckbox(R.id.checkbox_Propane, KEY_SHOW_PROPANE);
        setupCheckbox(R.id.checkbox_CO, KEY_SHOW_CO);
        setupCheckbox(R.id.checkbox_smoke, KEY_SHOW_SMOKE);
        setupCheckbox(R.id.checkbox_Alcohol, KEY_SHOW_ALCOHOL);
        setupCheckbox(R.id.checkbox_Methane, KEY_SHOW_METHANE);
        setupCheckbox(R.id.checkbox_H2, KEY_SHOW_H2);

    }
    private void setupCheckbox(int checkboxId, final String key) {
        CheckBox checkBox = findViewById(checkboxId);


        boolean savedValue = sharedPreferences.getBoolean(key, true);
        checkBox.setChecked(savedValue);


        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {

            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putBoolean(key, isChecked);

            editor.apply();


            if (isChecked) {
                Toast.makeText(SettingsActivity.this, buttonView.getText() + " enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(SettingsActivity.this, buttonView.getText() + " disabled", Toast.LENGTH_SHORT).show();
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
