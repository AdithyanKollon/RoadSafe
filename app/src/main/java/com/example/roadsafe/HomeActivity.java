package com.example.roadsafe;

import android.content.SharedPreferences;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.widget.Button;
import android.database.Cursor;
import android.net.Uri;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    Button btnCapture, btnViewMap, btnStats, btnLeaderboard;
    TextView tvTotal, tvAverage, tvWelcome, tvPoints, tvRiskBanner, tvPending, tvResolved;
    LinearLayout riskBannerContainer;

    PotholeDatabaseHelper dbHelper;
    NetworkReceiver networkReceiver = new NetworkReceiver();
    private FirebaseSyncHelper syncHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        dbHelper = new PotholeDatabaseHelper(this);
        syncHelper = new FirebaseSyncHelper(this);

        // ── Views ──────────────────────────────────────────────────────────
        tvTotal       = findViewById(R.id.tvTotal);
        tvAverage     = findViewById(R.id.tvAverage);
        tvWelcome     = findViewById(R.id.tvWelcome);
        tvPoints      = findViewById(R.id.tvPoints);
        tvRiskBanner  = findViewById(R.id.tvRiskBanner);
        tvPending     = findViewById(R.id.tvPending);
        tvResolved    = findViewById(R.id.tvResolved);
        riskBannerContainer = findViewById(R.id.riskBannerContainer);

        btnCapture    = findViewById(R.id.btnCapture);
        btnViewMap    = findViewById(R.id.btnViewMap);
        btnStats      = findViewById(R.id.btnStats);
        btnLeaderboard = findViewById(R.id.btnLeaderboard);

        // ── SharedPreferences ──────────────────────────────────────────────
        SharedPreferences prefs = getSharedPreferences("RoadSafePrefs", MODE_PRIVATE);
        String username = prefs.getString("username", "Guest");
        tvWelcome.setText("Welcome, " + username + "!");

        // ── Button clicks ──────────────────────────────────────────────────
        btnCapture.setOnClickListener(v ->
                startActivity(new Intent(this, CaptureActivity.class)));

        btnViewMap.setOnClickListener(v ->
                startActivity(new Intent(this, MapActivity.class)));

        btnStats.setOnClickListener(v ->
                startActivity(new Intent(this, StatsActivity.class)));

        btnLeaderboard.setOnClickListener(v ->
                startActivity(new Intent(this, LeaderboardActivity.class)));

        startService(new Intent(this, LocationService.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();

        syncHelper.syncFromFirebase(() -> runOnUiThread(this::loadDashboardData));
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────
    private void loadDashboardData() {

        // Pothole stats from ContentProvider
        Cursor cursor = getContentResolver().query(
                Uri.parse("content://com.example.roadsafe.provider/potholes"),
                null, null, null, null);

        int total = 0, severitySum = 0;
        int pending = 0, resolved = 0;

        if (cursor != null) {
            while (cursor.moveToNext()) {
                total++;
                String severity = cursor.getString(cursor.getColumnIndexOrThrow("severity"));
                String status   = cursor.getString(cursor.getColumnIndexOrThrow("status"));

                switch (severity) {
                    case "Low":    severitySum += 1; break;
                    case "Medium": severitySum += 2; break;
                    case "High":   severitySum += 3; break;
                }

                if ("Resolved".equals(status)) {
                    resolved++;
                } else {
                    pending++;
                }
            }
            cursor.close();
        }

        double average = total == 0 ? 0 : (double) severitySum / total;
        tvTotal.setText(String.valueOf(total));
        tvAverage.setText(String.format("%.1f", average));

        // Pending / Resolved counts
        tvPending.setText(String.valueOf(pending));
        tvResolved.setText(String.valueOf(resolved));

        // Risk level banner based on avg severity
        if (total == 0) {
            tvRiskBanner.setText("NO DATA");
            riskBannerContainer.setBackgroundColor(0xFF546E7A); // grey
        } else if (average <= 1.0) {
            tvRiskBanner.setText("LOW RISK");
            riskBannerContainer.setBackgroundColor(0xFF2E7D32); // green
        } else if (average <= 2.0) {
            tvRiskBanner.setText("MEDIUM RISK");
            riskBannerContainer.setBackgroundColor(0xFFE65100); // orange
        } else {
            tvRiskBanner.setText("HIGH RISK");
            riskBannerContainer.setBackgroundColor(0xFFC62828); // red
        }

        // Points from SQLite
        SharedPreferences prefs = getSharedPreferences("RoadSafePrefs", MODE_PRIVATE);
        String username = prefs.getString("username", "Guest");
        int pts = dbHelper.getPoints(username);
        tvPoints.setText(pts + " pts");
    }

    @Override protected void onStart() {
        super.onStart();
        registerReceiver(networkReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override protected void onStop() {
        super.onStop();
        unregisterReceiver(networkReceiver);
    }
}
