package com.example.gpscameraapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Picture;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.textview.MaterialTextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

public class MainActivity extends AppCompatActivity {


    public static final int CAMERA_AND_LOCATION_PERMISSION_REQUEST_CODE = 01;

    public static final int STORAGE_PERMISSION_REQUEST_CODE = 02;

    CameraManager cameraManager;

    private AutoFitTextureView previewForm;
    private Button btnTakePicture;

    private final String cameraLensFacing = "0";

    private CameraDevice cameraDevice;

    private CameraCaptureSession cameraCaptureSession;

    private CaptureRequest.Builder builder;

    private Size previewSize;

    private int mSensorOrientation;

    private static final int MAX_PREVIEW_WIDTH = 1920;

    private static final int MAX_PREVIEW_HEIGHT = 1080;

    File galleryFolder;

    PictureThread pictureThread;

    MaterialTextView txtLatitude, txtLongitude, txtCity;

    double latitude,longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        previewForm = findViewById(R.id.textureView);
        btnTakePicture = findViewById(R.id.button_take_picture);
        txtLatitude = findViewById(R.id.text_view_latitude);
        txtLongitude = findViewById(R.id.text_view_longitude);
        txtCity = findViewById(R.id.text_view_city);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideStatusBar();
        if (hasFeatureCamera()) {
            if (!isCameraAndLocationPermissionsGranted()) {
                requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION}, CAMERA_AND_LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                getUserLocation();
                if (previewForm.isAvailable()) {
                    startCamera(cameraLensFacing, previewForm.getWidth(), previewForm.getHeight());
                } else previewForm.setSurfaceTextureListener(surfaceTextureListener);
            }
        } else
            Toast.makeText(getApplicationContext(), "Sorry, Camera feature is necessary!", Toast.LENGTH_SHORT).show();

        btnTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isStoragePermissionGranted()) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST_CODE);
                } else {
                    pictureThread = new PictureThread(getApplicationContext(), previewForm, latitude,longitude,String.valueOf(txtCity.getText()));
                    pictureThread.start();
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        finishCamera();
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
            cameraDevice = null;
        }
    };

    private void createCameraPreview() throws CameraAccessException {
        SurfaceTexture surfaceTexture = previewForm.getSurfaceTexture();

        surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        Surface surface = new Surface(surfaceTexture);
        builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        builder.addTarget(surface);
        cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                if (cameraDevice == null) return;
                cameraCaptureSession = session;
                CameraCharacteristics characteristics = null;
                try {
                    characteristics = cameraManager.getCameraCharacteristics(cameraLensFacing);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
                int[] capabilities = characteristics
                        .get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);

                boolean isManualFocusSupported = IntStream.of(capabilities)
                        .anyMatch(x -> x == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR);

                if (isManualFocusSupported) {
                    builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
                    builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0f);
                }
                try {
                    cameraCaptureSession.setRepeatingRequest(builder.build(), null, null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                Toast.makeText(getApplicationContext(), "Something went wrong...", Toast.LENGTH_SHORT).show();
            }
        }, null);
    }

    TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            startCamera(cameraLensFacing, width, height);
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
    };

    @SuppressLint("MissingPermission")
    private void startCamera(String cameraLensFacing, int width, int height) {
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            setUpCameraOutputs(width, height);
            cameraManager.openCamera(cameraLensFacing, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void setUpCameraOutputs(int width, int height) {
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String id : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) continue;
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) continue;
                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea()
                );
                int displayRotation = getWindowManager().getDefaultDisplay().getRotation();
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e("TAG", "Display rotation is invalid: " + displayRotation);
                }
                Point displaySize = new Point();
                getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;
                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }

                previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedPreviewWidth,
                        rotatedPreviewHeight, maxPreviewWidth, maxPreviewHeight, largest);
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    previewForm.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
                } else {
                    previewForm.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                   int textureViewHeight, int maxWidth,
                                   int maxHeight, Size aspectRatio) {
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }
        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e("TAG", "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    private void finishCamera() {
        cameraDevice.close();
        cameraDevice = null;
    }

    private boolean hasFeatureCamera() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA) ? true : false;
    }

    private boolean isCameraAndLocationPermissionsGranted() {
        return checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ? true : false;
    }

    private boolean isStoragePermissionGranted() {
        return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ? true : false;
    }

    private void getUserLocation() {
        LocationManager locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage("Для работы приложения необходима служба геолокации, включить её?")
                    .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            MainActivity.this.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton("Нет", null)
                    .show();
        } else{
            LocationListener locListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    latitude = Math.round(location.getLatitude() *100000.0) / 100000.0;
                    longitude = Math.round(location.getLongitude() * 100000.0) / 100000.0;
                    txtLatitude.setText("Latitude: " + latitude);
                    txtLongitude.setText("Longitude: " + longitude);
                    Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
                    List<Address> addresses;
                    try {
                        addresses = gcd.getFromLocation(latitude,
                                longitude, 1);
                        if (addresses.size() > 0) {
                            txtCity.setText(addresses.get(0).getLocality());
                        }
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 5, locListener);
        }

    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==CAMERA_AND_LOCATION_PERMISSION_REQUEST_CODE){
            if (grantResults!=null){
                if (grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    startCamera(cameraLensFacing,previewForm.getWidth(),previewForm.getHeight());
                    getUserLocation();
                }
            }
        }
        else if (requestCode == STORAGE_PERMISSION_REQUEST_CODE){
            if (grantResults!=null){
                if (grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    pictureThread = new PictureThread(getApplicationContext(),previewForm,latitude,longitude,String.valueOf(txtCity.getText()));
                    pictureThread.start();
                }
            }
        }
    }
    private void hideStatusBar(){
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }
}