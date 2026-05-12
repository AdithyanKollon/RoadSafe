package com.example.roadsafe;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class LeaderboardActivity extends AppCompatActivity {

    ListView listLeaderboard;
    PotholeDatabaseHelper dbHelper;
    private String currentUsername = "Guest";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dbHelper         = new PotholeDatabaseHelper(this);
        listLeaderboard  = findViewById(R.id.listLeaderboard);

        SharedPreferences prefs = getSharedPreferences("RoadSafePrefs", MODE_PRIVATE);
        currentUsername = prefs.getString("username", "Guest");

        loadLeaderboard();
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    private void loadLeaderboard() {
        Cursor cursor = dbHelper.getLeaderboard();
        listLeaderboard.setAdapter(new LeaderboardAdapter(cursor));
    }

    // ── Adapter ───────────────────────────────────────────────────────────────
    private class LeaderboardAdapter extends BaseAdapter {

        private final Cursor cursor;
        private final int colName, colPts;

        LeaderboardAdapter(Cursor c) {
            this.cursor  = c;
            colName      = c.getColumnIndexOrThrow(PotholeDatabaseHelper.COL_USER_NAME);
            colPts       = c.getColumnIndexOrThrow(PotholeDatabaseHelper.COL_POINTS);
        }

        @Override public int getCount()              { return cursor == null ? 0 : cursor.getCount(); }
        @Override public Object getItem(int pos)     { return null; }
        @Override public long getItemId(int pos)     { return pos; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = LayoutInflater.from(LeaderboardActivity.this)
                        .inflate(R.layout.item_leaderboard, parent, false);
            }

            cursor.moveToPosition(position);

            TextView tvRank   = convertView.findViewById(R.id.tvRank);
            TextView tvName   = convertView.findViewById(R.id.tvName);
            TextView tvPoints = convertView.findViewById(R.id.tvPoints);
            TextView tvBadge  = convertView.findViewById(R.id.tvBadge);

            int rank = position + 1;
            String username = cursor.getString(colName);
            int points = cursor.getInt(colPts);

            // Rank display with medals
            tvRank.setText(rank <= 3
                    ? (rank == 1 ? "🥇" : rank == 2 ? "🥈" : "🥉")
                    : String.valueOf(rank));

            tvName.setText(username);
            tvPoints.setText(points + " pts");

            // Rank badge titles based on points
            tvBadge.setVisibility(View.VISIBLE);
            if (points >= 100) {
                tvBadge.setText("⭐ Elite Reporter");
                tvBadge.setTextColor(0xFFE65100);
            } else if (points >= 50) {
                tvBadge.setText("🔥 Active Reporter");
                tvBadge.setTextColor(0xFF1565C0);
            } else {
                tvBadge.setText("📋 Reporter");
                tvBadge.setTextColor(0xFF888888);
            }

            // Background color — top 3 + current user highlight
            boolean isCurrentUser = username != null && username.equals(currentUsername);

            if (isCurrentUser) {
                // Highlight current user with distinctive background
                convertView.setBackgroundColor(0xFFE3F2FD); // light blue
                tvName.setTextColor(0xFF1565C0);
            } else if (rank == 1) {
                convertView.setBackgroundColor(0xFFFFF8E1); // gold tint
                tvName.setTextColor(0xFF212121);
            } else if (rank == 2) {
                convertView.setBackgroundColor(0xFFF5F5F5); // silver tint
                tvName.setTextColor(0xFF212121);
            } else if (rank == 3) {
                convertView.setBackgroundColor(0xFFFFF3E0); // bronze tint
                tvName.setTextColor(0xFF212121);
            } else {
                convertView.setBackgroundColor(0xFFFFFFFF);
                tvName.setTextColor(0xFF212121);
            }

            return convertView;
        }
    }
}
