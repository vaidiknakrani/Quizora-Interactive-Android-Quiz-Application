package com.example.adminquiz.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import com.example.adminquiz.analytics.StreakCalculator;
import com.example.adminquiz.analytics.StreakCalculator.DayActivity;
import com.google.android.material.color.MaterialColors;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.TreeMap;

/**
 * Contribution-style calendar: one column per week, one row per weekday, shade = quizzes that day.
 * The number of weeks adapts to the width, so it fits phones and tablets without scrolling.
 */
public class HeatmapView extends BaseChartView {

    public interface OnDayClickListener {
        /** activity is null for a day without any quiz. */
        void onDayClick(long epochDay, @Nullable DayActivity activity);
    }

    private static final int[] LEVEL_ALPHA = {0, 85, 135, 195, 255};
    private static final String[] WEEKDAY = {"", "M", "", "W", "", "F", ""};

    private TreeMap<Long, DayActivity> days = new TreeMap<>();
    private long today = StreakCalculator.dayOf(System.currentTimeMillis(), TimeZone.getDefault());
    private long selected = Long.MIN_VALUE;
    private OnDayClickListener listener;

    private int weeks = 20;
    private float cell;
    private float gap;
    private float left;
    private float top;
    private final RectF rect = new RectF();
    private final SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());

    public HeatmapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        monthFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public void setData(TreeMap<Long, DayActivity> data, long todayEpochDay) {
        days = data == null ? new TreeMap<Long, DayActivity>() : data;
        today = todayEpochDay;
        invalidate();
    }

    public void setOnDayClickListener(OnDayClickListener l) {
        listener = l;
    }

    public void setSelectedDay(long epochDay) {
        selected = epochDay;
        invalidate();
    }

    private void computeLayout(int width) {
        gap = dp(3);
        left = getPaddingLeft() + dp(18);
        top = sp(10) + dp(8);
        float avail = width - left - getPaddingRight();
        float minCell = dp(14);
        weeks = Math.max(8, Math.min(53, (int) ((avail + gap) / (minCell + gap))));
        cell = Math.min(dp(26), (avail - (weeks - 1) * gap) / weeks);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getMode(widthSpec) == MeasureSpec.UNSPECIFIED
                ? (int) dp(320) : MeasureSpec.getSize(widthSpec);
        computeLayout(w);
        int h = (int) (top + 7 * cell + 6 * gap + getPaddingBottom() + dp(2));
        setMeasuredDimension(w, h);
    }

    private long firstColumnStart() {
        long lastColStart = today - StreakCalculator.dayOfWeek(today);
        return lastColStart - 7L * (weeks - 1);
    }

    private int levelOf(long day) {
        DayActivity a = days.get(day);
        return a == null ? 0 : StreakCalculator.level(a.quizzes);
    }

    private int colorFor(int level, int base) {
        return level == 0 ? base : ColorUtils.setAlphaComponent(cPrimary, LEVEL_ALPHA[level]);
    }

    /** Colour used for a heat level, shared with the legend row in the layout. */
    public int levelColor(int level) {
        resolveColors();
        int base = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceVariant, cGrid);
        return colorFor(Math.max(0, Math.min(4, level)), base);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        resolveColors();
        int base = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceVariant, cGrid);
        long first = firstColumnStart();
        float radius = Math.max(dp(2), cell * 0.2f);

        text.setTextSize(sp(9));
        text.setColor(cMuted);
        text.setTextAlign(Paint.Align.LEFT);
        for (int row = 0; row < 7; row++) {
            if (!WEEKDAY[row].isEmpty()) {
                canvas.drawText(WEEKDAY[row], getPaddingLeft(), top + row * (cell + gap) + cell * 0.75f, text);
            }
        }

        text.setTextSize(sp(10));
        int prevMonth = -1;
        float lastEnd = -1;
        java.util.Calendar cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        for (int col = 0; col < weeks; col++) {
            long colStart = first + 7L * col;
            cal.setTimeInMillis((colStart + 3) * StreakCalculator.DAY_MS);
            int month = cal.get(java.util.Calendar.MONTH);
            float x = left + col * (cell + gap);
            if (month != prevMonth) {
                String label = monthFormat.format(new java.util.Date(cal.getTimeInMillis()));
                if (x >= lastEnd) {
                    canvas.drawText(label, x, top - dp(5), text);
                    lastEnd = x + text.measureText(label) + dp(4);
                }
                prevMonth = month;
            }
            for (int row = 0; row < 7; row++) {
                long day = colStart + row;
                if (day > today) {
                    continue;
                }
                float y = top + row * (cell + gap);
                rect.set(x, y, x + cell, y + cell);
                fill.setColor(colorFor(levelOf(day), base));
                canvas.drawRoundRect(rect, radius, radius, fill);
                if (day == today) {
                    stroke.setColor(cPrimary);
                    stroke.setStrokeWidth(dp(1.5f));
                    canvas.drawRoundRect(rect, radius, radius, stroke);
                }
                if (day == selected) {
                    stroke.setColor(cText);
                    stroke.setStrokeWidth(dp(2));
                    canvas.drawRoundRect(rect, radius, radius, stroke);
                }
            }
        }
    }

    private long dayAt(float x, float y) {
        float step = cell + gap;
        int col = (int) ((x - left) / step);
        int row = (int) ((y - top) / step);
        if (x < left || y < top || col < 0 || col >= weeks || row < 0 || row > 6) {
            return Long.MIN_VALUE;
        }
        long day = firstColumnStart() + 7L * col + row;
        return day > today ? Long.MIN_VALUE : day;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_UP:
                long day = dayAt(e.getX(), e.getY());
                if (day != Long.MIN_VALUE) {
                    selected = day;
                    invalidate();
                    performClick();
                    if (listener != null) {
                        listener.onDayClick(day, days.get(day));
                    }
                }
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    /** Lets the screen show the day-detail card for a fresh selection without touching the grid. */
    public long getSelectedDay() {
        return selected;
    }

    public int visibleWeeks() {
        return weeks;
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) {
            invalidate();
        }
    }
}
