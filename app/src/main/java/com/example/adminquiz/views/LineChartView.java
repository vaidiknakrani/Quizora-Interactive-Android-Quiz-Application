package com.example.adminquiz.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Percent line chart (fixed 0-100 axis) with up to two series. Missing points are NaN: the line
 * joins the neighbouring real points and only real points get a dot.
 */
public class LineChartView extends BaseChartView {
    private List<String> labels = new ArrayList<>();
    private double[] first = new double[0];
    private double[] second;
    private String firstName;
    private String secondName;
    private final Path path = new Path();

    public LineChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        emptyText = "No data";
    }

    /** first is drawn in the theme colour, second (optional, may be null) in teal. */
    public void setData(List<String> xLabels, double[] firstSeries, String firstLabel,
                        double[] secondSeries, String secondLabel) {
        labels = xLabels == null ? new ArrayList<String>() : xLabels;
        first = firstSeries == null ? new double[0] : firstSeries;
        second = secondSeries;
        firstName = firstLabel;
        secondName = secondLabel;
        invalidate();
    }

    private static boolean hasValue(double[] s) {
        if (s == null) {
            return false;
        }
        for (double v : s) {
            if (!Double.isNaN(v)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        resolveColors();
        float w = getWidth();
        float h = getHeight();
        if (!hasValue(first) && !hasValue(second)) {
            drawEmpty(canvas, 0, h);
            return;
        }
        float left = dp(34);
        float right = dp(12);
        float top = dp(28);
        float bottom = dp(26);
        float chartW = w - left - right;
        float chartH = h - top - bottom;

        drawLegend(canvas, left, dp(2), new String[]{firstName, second == null ? null : secondName},
                new int[]{cPrimary, SECOND_SERIES});

        text.setTextSize(sp(10));
        stroke.setStrokeWidth(dp(1));
        for (int i = 0; i <= 4; i++) {
            float y = top + chartH * (1f - i / 4f);
            stroke.setColor(cGrid);
            canvas.drawLine(left, y, w - right, y, stroke);
            text.setColor(cMuted);
            text.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText((i * 25) + "%", left - dp(5), y + dp(3.5f), text);
        }

        int n = Math.max(first.length, second == null ? 0 : second.length);
        float maxLabel = 0;
        for (String s : labels) {
            maxLabel = Math.max(maxLabel, text.measureText(s));
        }
        float slot = n > 1 ? chartW / (n - 1) : chartW;
        int step = Math.max(1, (int) Math.ceil((maxLabel + dp(8)) / Math.max(1f, slot)));
        for (int i = 0; i < n && i < labels.size(); i++) {
            if (i % step == 0) {
                text.setColor(cMuted);
                text.setTextAlign(i == 0 && n > 1 ? Paint.Align.LEFT : Paint.Align.CENTER);
                float x = n > 1 ? left + slot * i : left + chartW / 2f;
                canvas.drawText(labels.get(i), x, h - dp(8), text);
            }
        }

        if (second != null) {
            drawSeries(canvas, second, SECOND_SERIES, left, top, chartW, chartH, n);
        }
        drawSeries(canvas, first, cPrimary, left, top, chartW, chartH, n);
    }

    private void drawSeries(Canvas canvas, double[] s, int color, float left, float top,
                            float chartW, float chartH, int n) {
        path.reset();
        boolean started = false;
        stroke.setColor(color);
        stroke.setStrokeWidth(dp(2.2f));
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        fill.setColor(color);
        for (int i = 0; i < s.length; i++) {
            if (Double.isNaN(s[i])) {
                continue;
            }
            float x = n > 1 ? left + chartW * i / (n - 1f) : left + chartW / 2f;
            float y = top + chartH * (1f - (float) (Math.max(0, Math.min(100, s[i])) / 100.0));
            if (!started) {
                path.moveTo(x, y);
                started = true;
            } else {
                path.lineTo(x, y);
            }
        }
        canvas.drawPath(path, stroke);
        for (int i = 0; i < s.length; i++) {
            if (Double.isNaN(s[i])) {
                continue;
            }
            float x = n > 1 ? left + chartW * i / (n - 1f) : left + chartW / 2f;
            float y = top + chartH * (1f - (float) (Math.max(0, Math.min(100, s[i])) / 100.0));
            canvas.drawCircle(x, y, dp(3.2f), fill);
        }
    }
}
