package com.example.roadsafe;
import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.location.Location;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import androidx.core.app.ActivityCompat;
import android.content.pm.PackageManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity {

    MapView map;
    FirebaseFirestore db;
    ListView listView;
    PotholeDatabaseHelper dbHelper;

    Button btnFilterAll, btnFilterLow, btnFilterMedium, btnFilterHigh;

    // Holds all SQLite row data for the list
    private final List<PotholeRow> allRows = new ArrayList<>();
    private final List<PotholeRow> filteredRows = new ArrayList<>();
    private PotholeListAdapter adapter;
    private Location myCurrentLocation = null;
    private String currentUsername = "Guest";
    private String currentFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_map);
        FusedLocationProviderClient locationClient =
                LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    myCurrentLocation = location;
                }
            });
        }
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        map      = findViewById(R.id.map);
        listView = findViewById(R.id.listPotholes);
        dbHelper = new PotholeDatabaseHelper(this);

        // Filter buttons
        btnFilterAll    = findViewById(R.id.btnFilterAll);
        btnFilterLow    = findViewById(R.id.btnFilterLow);
        btnFilterMedium = findViewById(R.id.btnFilterMedium);
        btnFilterHigh   = findViewById(R.id.btnFilterHigh);

        SharedPreferences prefs = getSharedPreferences("RoadSafePrefs", MODE_PRIVATE);
        currentUsername = prefs.getString("username", "Guest");

        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);

        db = FirebaseFirestore.getInstance();

        loadPotholesFromSQLite();
        loadPotholesFromFirebase();

        // Filter button listeners
        btnFilterAll.setOnClickListener(v -> applyFilter("All"));
        btnFilterLow.setOnClickListener(v -> applyFilter("Low"));
        btnFilterMedium.setOnClickListener(v -> applyFilter("Medium"));
        btnFilterHigh.setOnClickListener(v -> applyFilter("High"));
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    // ── Filter logic ─────────────────────────────────────────────────────────
    private void applyFilter(String severity) {
        currentFilter = severity;

        // Update button appearance
        updateFilterButtonStyle(btnFilterAll,    "All".equals(severity));
        updateFilterButtonStyle(btnFilterLow,    "Low".equals(severity));
        updateFilterButtonStyle(btnFilterMedium, "Medium".equals(severity));
        updateFilterButtonStyle(btnFilterHigh,   "High".equals(severity));

        // Filter rows
        filteredRows.clear();
        for (PotholeRow row : allRows) {
            if ("All".equals(severity) || row.severity.equals(severity)) {
                filteredRows.add(row);
            }
        }
        adapter.notifyDataSetChanged();

        // Refresh map markers
        map.getOverlays().clear();
        for (PotholeRow row : filteredRows) {
            addMarker(row.lat, row.lng, row.severity + " — " + row.status, null, row.status);
        }
        map.invalidate();
    }

    private void updateFilterButtonStyle(Button btn, boolean active) {
        if (active) {
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1565C0));
            btn.setTextColor(0xFFFFFFFF);
        } else {
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF37474F));
            btn.setTextColor(0xFFB0BEC5);
        }
    }

    // ── Load from SQLite ─────────────────────────────────────────────────────
    private void loadPotholesFromSQLite() {
        allRows.clear();
        filteredRows.clear();

        Cursor cursor = getContentResolver().query(
                Uri.parse("content://com.example.roadsafe.provider/potholes"),
                null, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                int    id       = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
                double lat      = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"));
                double lng      = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"));
                String severity = cursor.getString(cursor.getColumnIndexOrThrow("severity"));
                String status   = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                String username = cursor.getString(cursor.getColumnIndexOrThrow("username"));

                if (status == null) status = "Pending";
                if (username == null) username = "Guest";

                allRows.add(new PotholeRow(id, lat, lng, severity, status, username));
                addMarker(lat, lng, severity + " — " + status, null, status);
            }
            cursor.close();
        }

        filteredRows.addAll(allRows);
        adapter = new PotholeListAdapter();
        listView.setAdapter(adapter);

        // ── Tap: center map ──
        // ── Tap: show info ──
        listView.setOnItemClickListener((parent, view, position, id) -> {
            PotholeRow row = filteredRows.get(position);
            float distance = calculateDistance(row.lat, row.lng);

            String distanceText = distance >= 0
                    ? String.format("%.2f", distance) + " km"
                    : "Location unavailable";

            new AlertDialog.Builder(this)
                    .setTitle("Pothole Info")
                    .setMessage("Severity: "   + row.severity +
                            "\nStatus: "   + row.status +
                            "\nReporter: " + row.username +
                            "\nDistance: " + distanceText)
                    .setPositiveButton("OK", null)
                    .show();
            map.getController().setCenter(new GeoPoint(row.lat, row.lng));
        });

// ── Long press: update status ──
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showStatusDialog(position);
            return true;
        });
    }

    // ── Status update dialog ─────────────────────────────────────────────────
    private void showStatusDialog(int position) {
        PotholeRow row = filteredRows.get(position);

        new AlertDialog.Builder(this)
                .setTitle("Update Status")
                .setMessage("Current status: " + row.status + "\nReported by: " + row.username)
                .setPositiveButton("Mark as Resolved", (dialog, which) -> {
                    performStatusUpdate(row, "Resolved");
                })
                .setNeutralButton("Mark as Pending", (dialog, which) -> {
                    performStatusUpdate(row, "Pending");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performStatusUpdate(PotholeRow row, String newStatus) {
        if (newStatus.equals(row.status)) {
            Toast.makeText(this, "Status is already " + newStatus,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        boolean ok = dbHelper.updateStatus(row.id, newStatus, currentUsername);

        if (ok) {
            ContentValues cv = new ContentValues();
            cv.put("status", newStatus);
            getContentResolver().update(
                    Uri.parse("content://com.example.roadsafe.provider/potholes/" + row.id),
                    cv, null, null);

            row.status = newStatus;
            for (PotholeRow r : allRows) {
                if (r.id == row.id) { r.status = newStatus; break; }
            }
            adapter.notifyDataSetChanged();

            String msg = "Resolved".equals(newStatus)
                    ? "Marked Resolved ✅  +" + PotholeDatabaseHelper.POINTS_RESOLVE + " pts!"
                    : "Marked as Pending";

            new AlertDialog.Builder(this)
                    .setTitle("Status Updated")
                    .setMessage(msg)
                    .setPositiveButton("OK", null)
                    .show();
        } else {
            Toast.makeText(this, "Update failed — try again",
                    Toast.LENGTH_SHORT).show();
        }
    }
    // ── Firebase load ────────────────────────────────────────────────────────
    private void loadPotholesFromFirebase() {
        db.collection("potholes").get()
                .addOnSuccessListener(query -> runOnUiThread(() -> {
                    query.forEach(doc -> {
                        Double lat    = doc.getDouble("latitude");
                        Double lng    = doc.getDouble("longitude");
                        String sev    = doc.getString("severity");
                        String status = doc.getString("status");
                        String path   = doc.getString("imagePath");
                        if (lat != null && lng != null) {
                            addMarker(lat, lng,
                                    (sev != null ? sev : "Unknown") +
                                            " — " + (status != null ? status : "Pending"),
                                    path,
                                    status != null ? status : "Pending");
                        }
                    });
                }))
                .addOnFailureListener(e -> {
                    // Offline — markers already loaded from SQLite, nothing to do
                });
    }

    // ── Add marker ───────────────────────────────────────────────────────────
    private void addMarker(double lat, double lng,
                           String title, String imagePath, String status) {
        GeoPoint point  = new GeoPoint(lat, lng);
        Marker marker   = new Marker(map);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(title);

        if (imagePath != null) {
            marker.setOnMarkerClickListener((m, mapView) -> {
                showImageDialog(imagePath);
                return true;
            });
        }

        map.getOverlays().add(marker);
        map.getController().setCenter(point);
    }

    // ── Distance ─────────────────────────────────────────────────────────────
    private float calculateDistance(double lat, double lng) {
        Location pothole = new Location("pothole");
        pothole.setLatitude(lat);
        pothole.setLongitude(lng);

        if (myCurrentLocation != null) {
            return myCurrentLocation.distanceTo(pothole) / 1000f;
        }

        // Fallback if GPS not available yet
        return -1f;
    }

    // ── Image dialog ─────────────────────────────────────────────────────────
    private void showImageDialog(String imagePath) {
        File imgFile = new File(imagePath);
        if (!imgFile.exists()) return;
        Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(bitmap);
        new AlertDialog.Builder(this)
                .setTitle("Pothole Image")
                .setView(imageView)
                .setPositiveButton("Close", null)
                .show();
    }

    // ── Data model ───────────────────────────────────────────────────────────
    static class PotholeRow {
        int id; double lat, lng; String severity, status, username;
        PotholeRow(int id, double lat, double lng, String severity, String status, String username) {
            this.id = id; this.lat = lat; this.lng = lng;
            this.severity = severity; this.status = status; this.username = username;
        }
    }

    // ── List adapter ─────────────────────────────────────────────────────────
    private class PotholeListAdapter extends BaseAdapter {

        @Override public int getCount()           { return filteredRows.size(); }
        @Override public Object getItem(int pos)  { return filteredRows.get(pos); }
        @Override public long getItemId(int pos)  { return filteredRows.get(pos).id; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MapActivity.this)
                        .inflate(R.layout.item_pothole_map, parent, false);
            }

            PotholeRow row = filteredRows.get(position);

            TextView tvSeverity = convertView.findViewById(R.id.tvItemSeverity);
            TextView tvStatus   = convertView.findViewById(R.id.tvItemStatus);
            TextView tvCoords   = convertView.findViewById(R.id.tvItemCoords);

            tvSeverity.setText(row.severity);
            tvStatus.setText(row.status);
            tvCoords.setText(String.format("%.4f, %.4f", row.lat, row.lng));

            // Color badge for status
            int statusColor = "Resolved".equals(row.status) ? 0xFF2E7D32 : 0xFFE65100;
            tvStatus.setBackgroundColor(statusColor);
            tvStatus.setTextColor(0xFFFFFFFF);

            return convertView;
        }
    }
}
