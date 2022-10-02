package com.example.gpscameraapp;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    public static final int PERMISSIONS_REQUEST_CODE = 100;

    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (hasFeatureCamera()){
            if (isPermissionGranted()){

            }
            else requestPermissions(new String[] {Manifest.permission.CAMERA},PERMISSIONS_REQUEST_CODE);
        }
        else Toast.makeText(getApplicationContext(), "Sorry, Camera feature is necessary!", Toast.LENGTH_SHORT).show();
    }

    private boolean hasFeatureCamera(){
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA) ? true : false;
    }

    private boolean isPermissionGranted(){
        return checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED ? true : false;
    }
}