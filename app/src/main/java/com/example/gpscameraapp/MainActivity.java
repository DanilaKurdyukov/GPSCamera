package com.example.gpscameraapp;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    public static final int PERMISSIONS_REQUEST_CODE = 100;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    private boolean hasFeatureAndPermissions(){
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)
                && checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED){
            return true;
        }
        else return false;
    }
    private void requestPermissions(){

    }
}