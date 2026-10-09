package com.example.adminquiz.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

import com.google.android.material.color.MaterialColors;

import java.util.Locale;

/** Shared paints, theme colours (so dark mode just works) and helpers for the chart views. */
abstract class BaseChartView extends View {
    static final int SECOND_SERIES = 0xFF00A8CC;

    protected final TextPaint text = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    protected final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    protected final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);

    protected int cText = Color.DKGRAY;
    protected int cMuted = Color.GRAY;
    protected int cGrid = Color.LTGRAY;
    protected int cPrimary = 0xFF9D05FF;
    protected int cEmpty = Color.LTGRAY;
    protected String emptyText = "";

    BaseChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        stroke.setStyle(Paint.Style.STROKE);
        fill.setStyle(Paint.Style.FILL);
    }

    protected float dp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    protected float sp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, getResources().getDisplayMetrics());
    }

    protected void resolveColors() {
        cText = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, Color.DKGRAY);
        cMuted = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.GRAY);
        cGrid = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutlineVariant, Color.LTGRAY);
        cPrimary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, 0xFF9D05FF);
        cEmpty = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceVariant, Color.LTGRAY);
    }

    public void setEmptyText(String message) {
        emptyText = message == null ? "" : message;
        invalidate();
    }

    protected void drawEmpty(Canvas canvas, float top, float bottom) {
        text.setColor(cMuted);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(sp(13));
        float cy = (top + bottom) / 2f - (text.descent() + text.ascent()) / 2f;
        canvas.drawText(emptyText, getWidth() / 2f, cy, text);
    }

    protected static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.US, "%.1f", v);
    }

    /** Largest multiple of 4 steps above the data so grid lines fall on whole numbers. */
    protected static double niceMax(double m) {
        if (m <= 4) {
            return 4;
        }
        if (m <= 40) {
            return Math.ceil(m / 4.0) * 4;
        }
        if (m <= 400) {
            return Math.ceil(m / 40.0) * 40;
        }
        return Math.ceil(m / 400.0) * 400;
    }

    /** Draws coloured dots with names in one row; returns the row height used. */
    protected float drawLegend(Canvas canvas, float x, float y, String[] names, int[] colors) {
        text.setTextSize(sp(11));
        text.setTextAlign(Paint.Align.LEFT);
        float cx = x;
        float baseline = y + dp(10);
        for (int i = 0; i < names.length; i++) {
            if (names[i] == null) {
                continue;
            }
            fill.setColor(colors[i]);
            canvas.drawCircle(cx + dp(4), baseline - dp(4), dp(4), fill);
            text.setColor(cMuted);
            canvas.drawText(names[i], cx + dp(12), baseline, text);
            cx += dp(12) + text.measureText(names[i]) + dp(14);
        }
        return dp(18);
    }
}
