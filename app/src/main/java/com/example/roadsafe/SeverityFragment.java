package com.example.roadsafe;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SeverityFragment extends Fragment {

    private TextView tvTotal, tvHigh, tvMedium, tvLow;
    private TextView tvResolvedPercent, tvWeeklyTrend, tvMostCommon;
    private ProgressBar progressResolved;
    private PotholeDatabaseHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_severity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTotal           = view.findViewById(R.id.tvTotal);
        tvHigh            = view.findViewById(R.id.tvHigh);
        tvMedium          = view.findViewById(R.id.tvMedium);
        tvLow             = view.findViewById(R.id.tvLow);
        tvResolvedPercent = view.findViewById(R.id.tvResolvedPercent);
        tvWeeklyTrend     = view.findViewById(R.id.tvWeeklyTrend);
        tvMostCommon      = view.findViewById(R.id.tvMostCommon);
        progressResolved  = view.findViewById(R.id.progressResolved);

        dbHelper = new PotholeDatabaseHelper(requireContext());
        loadStats();
    }

    private void loadStats() {
        int total  = getCount(null);
        int high   = getCount("High");
        int medium = getCount("Medium");
        int low    = getCount("Low");

        tvTotal.setText(String.valueOf(total));
        tvHigh.setText(String.valueOf(high));
        tvMedium.setText(String.valueOf(medium));
        tvLow.setText(String.valueOf(low));

        // Resolved % progress bar
        int resolved = dbHelper.getResolvedCount();
        int resolvedPercent = total == 0 ? 0 : (int) ((resolved * 100.0) / total);
        tvResolvedPercent.setText(resolvedPercent + "%");
        progressResolved.setProgress(resolvedPercent);

        // Weekly trend arrow
        int thisWeek = dbHelper.getThisWeekCount();
        int lastWeek = dbHelper.getLastWeekCount();
        if (thisWeek > lastWeek) {
            tvWeeklyTrend.setText("↑ " + thisWeek);
            tvWeeklyTrend.setTextColor(0xFFC62828);
        } else if (thisWeek < lastWeek) {
            tvWeeklyTrend.setText("↓ " + thisWeek);
            tvWeeklyTrend.setTextColor(0xFF2E7D32);
        } else {
            tvWeeklyTrend.setText("→ " + thisWeek);
            tvWeeklyTrend.setTextColor(0xFF1565C0);
        }

        // Most common severity callout
        String mostCommon = dbHelper.getMostCommonSeverity();
        tvMostCommon.setText(mostCommon);
        switch (mostCommon) {
            case "High":   tvMostCommon.setTextColor(0xFFC62828); break;
            case "Medium": tvMostCommon.setTextColor(0xFFE65100); break;
            case "Low":    tvMostCommon.setTextColor(0xFF2E7D32); break;
            default:       tvMostCommon.setTextColor(0xFF888888); break;
        }
    }

    private int getCount(String severity) {
        String selection = null;
        String[] selectionArgs = null;
        if (severity != null) {
            selection     = "severity=?";
            selectionArgs = new String[]{severity};
        }
        Cursor cursor = requireContext().getContentResolver().query(
                Uri.parse("content://com.example.roadsafe.provider/potholes"),
                null, selection, selectionArgs, null);
        int count = 0;
        if (cursor != null) { count = cursor.getCount(); cursor.close(); }
        return count;
    }
}
