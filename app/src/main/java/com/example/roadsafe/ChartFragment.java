package com.example.roadsafe;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class ChartFragment extends Fragment {

    private LinearLayout chartContainer;
    private PotholeDatabaseHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chartContainer = view.findViewById(R.id.chartContainer);
        dbHelper = new PotholeDatabaseHelper(requireContext());
        loadBarChart();
    }

    private void loadBarChart() {
        List<String>  labels = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        Cursor c = dbHelper.getReportsPerDay();
        if (c != null) {
            while (c.moveToNext()) {
                labels.add(c.getString(0));
                values.add(c.getInt(1));
            }
            c.close();
        }

        if (labels.isEmpty()) {
            // Placeholder data so chart is always visible
            labels.add("Mon"); values.add(2);
            labels.add("Tue"); values.add(5);
            labels.add("Wed"); values.add(3);
            labels.add("Thu"); values.add(7);
            labels.add("Fri"); values.add(4);
            labels.add("Sat"); values.add(6);
            labels.add("Sun"); values.add(1);
        }

        StatsActivity.BarChartView chart =
                new StatsActivity.BarChartView(requireContext(), labels, values);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 600);
        chart.setLayoutParams(params);
        chartContainer.addView(chart);
    }
}
