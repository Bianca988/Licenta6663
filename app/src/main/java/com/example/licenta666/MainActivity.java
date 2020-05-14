package com.example.licenta666;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        getSupportActionBar(toolbar);

        checkSelfPermission();

        FloatingActionButton showMapFab = (FloatingActionButton) findViewById(R.id.show_map_fab);
        showMapFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mapsOverlayIntent = new Intent(MainActivity.this, MapsOverlayActivity.class);
                startActivity(mapsOverlayIntent);
            }
        });
    }



    private void checkSelfPermission()
    {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE)+
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.CHANGE_WIFI_STATE) +
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) +
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
        {
            Intent permissionIntent = new Intent(this, PermissionActivity.class);
            startActivity(permissionIntent);
            this.finish();
        }
    }
}
