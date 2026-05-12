package com.example.roadsafe;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import java.util.ArrayList;
import java.util.List;

public class StatsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ViewPager2 viewPager = findViewById(R.id.viewPager);
        TabLayout  tabLayout = findViewById(R.id.tabLayout);

        viewPager.setAdapter(new StatsPagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "Severity" : "Daily Chart");
        }).attach();
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }

    // ── Pager adapter ─────────────────────────────────────────────────────────
    private static class StatsPagerAdapter extends FragmentStateAdapter {
        StatsPagerAdapter(FragmentActivity fa) { super(fa); }

        @Override public int getItemCount() { return 2; }

        @Override
        public Fragment createFragment(int position) {
            return position == 0 ? new SeverityFragment() : new ChartFragment();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Custom Canvas Bar Chart  (used by ChartFragment)
    // ═══════════════════════════════════════════════════════════════════════════
    public static class BarChartView extends View {

        private final List<String>  labels;
        private final List<Integer> values;

        private final Paint barPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint axisPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint gridPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private static final int COLOR_AXIS  = 0xFF444444;
        private static final int COLOR_GRID  = 0xFFE0E0E0;
        private static final int COLOR_LABEL = 0xFF666666;
        private static final int COLOR_VALUE = 0xFF1A237E;
        private static final int COLOR_TITLE = 0xFF1A237E;

        public BarChartView(Context context, List<String> labels, List<Integer> values) {
            super(context);
            this.labels = labels;
            this.values = values;
            init();
        }

        public BarChartView(Context context, AttributeSet attrs) {
            super(context, attrs);
            this.labels = new ArrayList<>();
            this.values = new ArrayList<>();
            init();
        }

        private void init() {
            axisPaint.setColor(COLOR_AXIS);
            axisPaint.setStrokeWidth(3f);
            axisPaint.setStyle(Paint.Style.STROKE);
            gridPaint.setColor(COLOR_GRID);
            gridPaint.setStrokeWidth(1f);
            barPaint.setStyle(Paint.Style.FILL);
            labelPaint.setColor(COLOR_LABEL);
            labelPaint.setTextSize(28f);
            labelPaint.setTextAlign(Paint.Align.CENTER);
            valuePaint.setColor(COLOR_VALUE);
            valuePaint.setTextSize(26f);
            valuePaint.setTextAlign(Paint.Align.CENTER);
            valuePaint.setFakeBoldText(true);
            titlePaint.setColor(COLOR_TITLE);
            titlePaint.setTextSize(32f);
            titlePaint.setFakeBoldText(true);
            titlePaint.setTextAlign(Paint.Align.LEFT);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (values.isEmpty()) return;

            float w = getWidth(), h = getHeight();
            float chartLeft   = 60f,  chartRight  = w - 30f;
            float chartTop    = 80f,  chartBottom = h - 80f;
            float chartH      = chartBottom - chartTop;
            float chartW      = chartRight  - chartLeft;

            canvas.drawText("Reports Per Day (Last 7 Days)",
                    chartLeft, chartTop - 20f, titlePaint);

            int maxVal = 1;
            for (int v : values) if (v > maxVal) maxVal = v;

            for (int i = 1; i <= 4; i++) {
                float y = chartBottom - (chartH * i / 4f);
                canvas.drawLine(chartLeft, y, chartRight, y, gridPaint);
                Paint yLabel = new Paint(labelPaint);
                yLabel.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText(String.valueOf(maxVal * i / 4),
                        chartLeft - 8f, y + 10f, yLabel);
            }

            canvas.drawLine(chartLeft, chartTop,    chartLeft,  chartBottom, axisPaint);
            canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, axisPaint);

            int n = values.size();
            float barWidth  = (chartW / n) * 0.55f;
            float slotWidth = chartW / n;
            int[] barColors = {
                    0xFF1565C0, 0xFF1976D2, 0xFF1E88E5,
                    0xFF42A5F5, 0xFF64B5F6, 0xFF2E7D32, 0xFF43A047
            };

            for (int i = 0; i < n; i++) {
                float barH = (values.get(i) / (float) maxVal) * chartH;
                float left = chartLeft + i * slotWidth + (slotWidth - barWidth) / 2f;
                float right = left + barWidth;
                float top   = chartBottom - barH;

                barPaint.setColor(barColors[i % barColors.length]);
                canvas.drawRoundRect(new RectF(left, top, right, chartBottom), 12f, 12f, barPaint);
                canvas.drawRect(new RectF(left, top + 12f, right, chartBottom), barPaint);
                canvas.drawText(String.valueOf(values.get(i)),
                        left + barWidth / 2f, top - 8f, valuePaint);
                canvas.drawText(labels.get(i),
                        left + barWidth / 2f, chartBottom + 40f, labelPaint);
            }
        }
    }
}