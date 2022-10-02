package com.example.gpscameraapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.view.TextureView;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    public static final int PERMISSIONS_REQUEST_CODE = 100;

    CameraManager cameraManager;

    private TextureView previewForm;
    private Button btnTakePicture;

    private String cameraLensFacing = "0";

    private CameraDevice cameraDevice;

    private CameraCaptureSession cameraCaptureSession;

    private CaptureRequest.Builder builder;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewForm = findViewById(R.id.textureView);
        btnTakePicture = findViewById(R.id.button_take_picture);
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


    CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

        }
    }

    private void createCameraPreview() {
    }


    private void finishCamera(){
        cameraDevice.close();
        cameraDevice = null;
    }

    private boolean hasFeatureCamera(){
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA) ? true : false;
    }

    private boolean isPermissionGranted(){
        return checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED ? true : false;
    }
}