package com.example.roadsafe;

import androidx.appcompat.widget.Toolbar;
import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.net.Uri;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.Spinner;
import android.app.AlertDialog;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.widget.RatingBar;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

public class CaptureActivity extends AppCompatActivity {

    RatingBar ratingBar;
    ImageView imgPreview;
    Button btnOpenCamera, btnSave;
    Bitmap capturedImage;
    Spinner spinnerSeverity;

    FirebaseFirestore db;
    FusedLocationProviderClient locationClient;
    PotholeDatabaseHelper dbHelper;

    private String currentUsername = "Guest";

    private static final int CAMERA_PERMISSION_CODE   = 101;
    private static final int LOCATION_PERMISSION_CODE = 102;

    ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK &&
                                result.getData() != null &&
                                result.getData().getExtras() != null &&
                                result.getData().getExtras().get("data") != null) {

                            capturedImage = (Bitmap) result.getData().getExtras().get("data");
                            imgPreview.setImageBitmap(capturedImage);
                        } else {
                            Toast.makeText(this, "Image capture failed, try again",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        ratingBar       = findViewById(R.id.ratingBar);
        imgPreview      = findViewById(R.id.imgPreview);
        btnOpenCamera   = findViewById(R.id.btnOpenCamera);
        btnSave         = findViewById(R.id.btnSave);
        spinnerSeverity = findViewById(R.id.spinnerSeverity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db         = FirebaseFirestore.getInstance();
        dbHelper   = new PotholeDatabaseHelper(this);
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        // Get logged-in username
        SharedPreferences prefs = getSharedPreferences("RoadSafePrefs", MODE_PRIVATE);
        currentUsername = prefs.getString("username", "Guest");

        requestLocationPermission();

        // Auto-suggest severity from rating
        ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
            if (fromUser) {
                int ratingInt = (int) rating;
                if (ratingInt <= 2) {
                    spinnerSeverity.setSelection(0); // Low
                } else if (ratingInt == 3) {
                    spinnerSeverity.setSelection(1); // Medium
                } else {
                    spinnerSeverity.setSelection(2); // High
                }
            }
        });

        btnOpenCamera.setOnClickListener(v -> checkCameraPermission());
        btnSave.setOnClickListener(v -> savePothole());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            openCamera();
        }
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                }, LOCATION_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }

    private void savePothole() {
        // Block save if no image
        if (capturedImage == null) {
            Toast.makeText(this, "Capture an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Enforce minimum 1 star rating
        if (ratingBar.getRating() < 1) {
            Toast.makeText(this, "Please give at least 1 star rating", Toast.LENGTH_SHORT).show();
            return;
        }

        String imagePath = saveImageToInternalStorage(capturedImage);

        locationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) {
                Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show();
                return;
            }

            double lat      = location.getLatitude();
            double lng      = location.getLongitude();
            String severity = spinnerSeverity.getSelectedItem().toString();

            // ── Duplicate within 50m warning ─────────────────────────────────
            Cursor nearbyCursor = dbHelper.getPotholesNearby(lat, lng, 50);
            int nearbyCount = 0;
            if (nearbyCursor != null) {
                nearbyCount = nearbyCursor.getCount();
                nearbyCursor.close();
            }

            if (nearbyCount > 0) {
                new AlertDialog.Builder(this)
                        .setTitle("⚠️ Duplicate Warning")
                        .setMessage("There " + (nearbyCount == 1 ? "is 1 pothole" : "are " + nearbyCount + " potholes")
                                + " reported within 50m of this location.\n\nDo you want to save anyway?")
                        .setPositiveButton("Save Anyway", (dialog, which) -> {
                            performSave(lat, lng, severity, imagePath);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            } else {
                performSave(lat, lng, severity, imagePath);
            }
        });
    }

    private void performSave(double lat, double lng, String severity, String imagePath) {
        int severityValue;
        switch (severity) {
            case "Medium": severityValue = 2; break;
            case "High":   severityValue = 3; break;
            default:       severityValue = 1; break;
        }

        int ratingValue  = (int) ratingBar.getRating();
        int impactScore  = severityValue * ratingValue;
        long timestamp   = System.currentTimeMillis();

        // ── 1. Save to SQLite (offline-safe) ─────────────────────────────
        boolean saved = saveToSQLite(lat, lng, severity, impactScore, timestamp);

        if (saved) {
            // Award points for uploading
            dbHelper.addPoints(currentUsername, PotholeDatabaseHelper.POINTS_UPLOAD);
        }

        // ── 2. Save to Firebase ───────────────────────────────────────────
        Map<String, Object> data = new HashMap<>();
        data.put("latitude",  lat);
        data.put("longitude", lng);
        data.put("imagePath", imagePath);
        data.put("severity",  severity);
        data.put("status",    "Pending");
        data.put("timestamp", timestamp);
        data.put("username",  currentUsername);

        db.collection("potholes")
                .add(data)
                .addOnSuccessListener(doc ->
                        Toast.makeText(this, "Saved locally & to cloud", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Saved locally (No Internet)", Toast.LENGTH_SHORT).show());

        // ── 3. Result dialog ──────────────────────────────────────────────
        int newPoints = dbHelper.getPoints(currentUsername);
        new AlertDialog.Builder(this)
                .setTitle("✅ Report Submitted")
                .setMessage(
                        "Impact Score: " + impactScore + "\n" +
                        "+" + PotholeDatabaseHelper.POINTS_UPLOAD + " pts awarded\n\n" +
                        "Your total: " + newPoints + " pts")
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    // ── SQLite insert ─────────────────────────────────────────────────────────
    private boolean saveToSQLite(double lat, double lng,
                                  String severity, int impactScore,
                                  long timestamp) {
        ContentValues values = new ContentValues();
        values.put("latitude",    lat);
        values.put("longitude",   lng);
        values.put("severity",    severity);
        values.put("impactScore", impactScore);
        values.put("status",      "Pending");
        values.put("timestamp",   timestamp);
        values.put("username",    currentUsername);

        Uri uri = getContentResolver().insert(
                Uri.parse("content://com.example.roadsafe.provider/potholes"), values);

        return uri != null;
    }

    // ── Image storage ─────────────────────────────────────────────────────────
    private String saveImageToInternalStorage(Bitmap bitmap) {
        File directory = getDir("potholes", MODE_PRIVATE);
        String fileName   = "pothole_" + System.currentTimeMillis() + ".jpg";
        File imageFile    = new File(directory, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imageFile.getAbsolutePath();
    }
}
