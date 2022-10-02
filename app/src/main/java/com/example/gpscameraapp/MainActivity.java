package com.example.gpscameraapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Button;
import android.widget.Toast;

import java.util.Arrays;

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
            try {
                createCameraPreview();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice =null;
        }
    };

    private void createCameraPreview() throws CameraAccessException {
        SurfaceTexture surfaceTexture = previewForm.getSurfaceTexture();
        Surface surface = new Surface(surfaceTexture);
        builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        builder.addTarget(surface);
        cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                if (cameraDevice==null) return;
                cameraCaptureSession = session;
                builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                try {
                    cameraCaptureSession.setRepeatingRequest(builder.build(),null,null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                Toast.makeText(getApplicationContext(), "Something went wrong...", Toast.LENGTH_SHORT).show();
            }
        },null);
    }

    TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            startCamera(cameraLensFacing);
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

        }
    }

    private void startCamera(String cameraLensFacing) {
        
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==PERMISSIONS_REQUEST_CODE){
            if (grantResults!=null){
                if (grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    startCamera(cameraLensFacing);
                }
            }
        }
    }
}