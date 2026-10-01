package com.example.translationapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Settings UI will be handled by system settings
        // This activity exists to satisfy the accessibility service requirement
        setContentView(R.layout.activity_settings);
    }
}
