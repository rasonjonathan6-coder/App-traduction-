package com.example.translationapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Switch;
import android.widget.CompoundButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {

    private static final int OVERLAY_PERMISSION_REQ_CODE = 1;
    private Switch translationApiSwitch;
    private Button openSettingsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        translationApiSwitch = findViewById(R.id.translationApiSwitch);
        openSettingsButton = findViewById(R.id.openSettingsButton);

        // Check if we have overlay permission
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission();
        }

        // Set up translation API switch
        translationApiSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Save preference for translation API choice
                getSharedPreferences("translation_prefs", MODE_PRIVATE)
                        .edit()
                        .putBoolean("use_google_translate", isChecked)
                        .apply();
            }
        });

        // Load saved preference
        boolean useGoogle = getSharedPreferences("translation_prefs", MODE_PRIVATE)
                .getBoolean("use_google_translate", true);
        translationApiSwitch.setChecked(useGoogle);

        // Open settings button
        openSettingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    // Permission not granted
                }
            }
        }
    }
}
