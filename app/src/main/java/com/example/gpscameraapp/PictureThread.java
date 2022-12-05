package com.example.gpscameraapp;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.TextureView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class PictureThread extends Thread{

    File galleryFolder;
    Context context;
    TextureView previewForm;
    Uri images;
    double latitude, longitude;

    public PictureThread(Context context, TextureView previewForm, double latitude, double longitude) {
        this.context = context;
        this.previewForm = previewForm;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public void run() {
        super.run();
        try {
            saveImage(editImage(previewForm.getBitmap(),String.valueOf(latitude),String.valueOf(longitude)),"asdasdsa");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveImage(Bitmap bitmap, @NonNull String name) throws IOException {
        boolean saved;
        OutputStream fos;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            fos = resolver.openOutputStream(imageUri);
        } else {
            createImageGallery();
            String imagesDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM).toString() + File.separator + galleryFolder.getName();

            File file = new File(imagesDir);

            if (!file.exists()) {
                file.mkdir();
            }

            File image = new File(imagesDir, name + ".png");
            fos = new FileOutputStream(image);

        }

        saved = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        fos.flush();
        fos.close();
    }

    public void createImageGallery() {
        File storageDirectory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        galleryFolder = new File(storageDirectory,context.getResources().getString(R.string.app_name));
        if (!galleryFolder.exists()){
            boolean wasCreated = galleryFolder.mkdirs();
            if (!wasCreated){
                Toast.makeText(context, "Failed to create directory.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Bitmap editImage(Bitmap image,String latitude,String longitude){
        Typeface tf = Typeface.create("Helvetica",Typeface.BOLD);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTypeface(tf);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(80);

        Rect textRect = new Rect();
        paint.getTextBounds(latitude,0,latitude.length(),textRect);

        Canvas canvas = new Canvas(image);

        canvas.drawText("Latitude: " + latitude,canvas.getWidth() / 9,previewForm.getHeight()-700,paint);

        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTypeface(tf);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(80);
        paint.getTextBounds(longitude,0,longitude.length(),textRect);

        canvas.drawText("Longitude : " + longitude,canvas.getWidth() / 9,previewForm.getHeight()-500,paint);
        return image;
    }

    private void saveImageToGallery(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            images = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        } else{
            images = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }
        OutputStream outputPhoto = null;
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "ImageName");
        values.put(MediaStore.Images.Media.DESCRIPTION, "ImageDescription");
        values.put(MediaStore.Images.Media.MIME_TYPE,"images/*");
        Uri fileUri = context.getContentResolver().insert(images,values);
        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        try {
            outputPhoto = context.getContentResolver().openOutputStream(fileUri);
            previewForm.getBitmap().compress(Bitmap.CompressFormat.JPEG, 100, outputPhoto);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (outputPhoto != null) {
                try {
                    outputPhoto.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
