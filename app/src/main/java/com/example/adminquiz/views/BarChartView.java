package com.example.adminquiz.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Vertical bars with a 4-step grid, value labels and automatic label thinning. */
public class BarChartView extends BaseChartView {
    private List<String> labels = new ArrayList<>();
    private double[] values = new double[0];
    private final RectF rect = new RectF();

    public BarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        emptyText = "No data";
    }

    public void setData(List<String> xLabels, double[] data) {
        labels = xLabels == null ? new ArrayList<String>() : xLabels;
        values = data == null ? new double[0] : data;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        resolveColors();
        float w = getWidth();
        float h = getHeight();
        double max = 0;
        for (double v : values) {
            if (!Double.isNaN(v)) {
                max = Math.max(max, v);
            }
        }
        if (values.length == 0 || max <= 0) {
            drawEmpty(canvas, 0, h);
            return;
        }
        float left = dp(34);
        float right = dp(8);
        float top = dp(18);
        float bottom = dp(26);
        float chartW = w - left - right;
        float chartH = h - top - bottom;
        double axisMax = niceMax(max);

        text.setTextSize(sp(10));
        stroke.setStrokeWidth(dp(1));
        for (int i = 0; i <= 4; i++) {
            float y = top + chartH * (1f - i / 4f);
            stroke.setColor(cGrid);
            canvas.drawLine(left, y, w - right, y, stroke);
            text.setColor(cMuted);
            text.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(fmt(axisMax * i / 4.0), left - dp(5), y + dp(3.5f), text);
        }

        int n = values.length;
        float slot = chartW / n;
        float barW = Math.min(slot * 0.68f, dp(40));
        float maxLabel = 0;
        for (String s : labels) {
            maxLabel = Math.max(maxLabel, text.measureText(s));
        }
        int step = Math.max(1, (int) Math.ceil((maxLabel + dp(6)) / slot));

        for (int i = 0; i < n; i++) {
            double v = Double.isNaN(values[i]) ? 0 : values[i];
            float cx = left + slot * i + slot / 2f;
            float barH = (float) (v / axisMax * chartH);
            if (barH > 0) {
                rect.set(cx - barW / 2f, top + chartH - barH, cx + barW / 2f, top + chartH);
                fill.setColor(cPrimary);
                canvas.drawRoundRect(rect, dp(3), dp(3), fill);
                if (slot >= dp(22) && v > 0) {
                    text.setColor(cText);
                    text.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText(fmt(v), cx, rect.top - dp(4), text);
                }
            }
            if (i % step == 0 && i < labels.size()) {
                text.setColor(cMuted);
                text.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(labels.get(i), cx, h - dp(8), text);
            }
        }
    }
}
