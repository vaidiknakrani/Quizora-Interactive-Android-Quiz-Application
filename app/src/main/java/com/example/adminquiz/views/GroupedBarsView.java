package com.example.adminquiz.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Horizontal percent bars, one block per subject or quiz: class average and (optionally) you. */
public class GroupedBarsView extends BaseChartView {
    private List<String> labels = new ArrayList<>();
    private double[] classValues = new double[0];
    private double[] yourValues = new double[0];
    private String className = "Class";
    private String yourName = "You";
    private final RectF rect = new RectF();

    public GroupedBarsView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        emptyText = "No data";
    }

    public void setData(List<String> rowLabels, double[] classPct, double[] yourPct,
                        String classLabel, String yourLabel) {
        labels = rowLabels == null ? new ArrayList<String>() : rowLabels;
        classValues = classPct == null ? new double[0] : classPct;
        yourValues = yourPct == null ? new double[0] : yourPct;
        className = classLabel;
        yourName = yourLabel;
        requestLayout();
        invalidate();
    }

    private boolean hasYours() {
        for (double v : yourValues) {
            if (!Double.isNaN(v)) {
                return true;
            }
        }
        return false;
    }

    private float rowHeight() {
        return sp(14) + dp(6) + (hasYours() ? dp(10) * 2 + dp(4) : dp(10)) + dp(12);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getMode(widthSpec) == MeasureSpec.UNSPECIFIED
                ? (int) dp(320) : MeasureSpec.getSize(widthSpec);
        int h = classValues.length == 0 ? (int) dp(90)
                : (int) (dp(24) + classValues.length * rowHeight() + dp(4));
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        resolveColors();
        if (classValues.length == 0) {
            drawEmpty(canvas, 0, getHeight());
            return;
        }
        float w = getWidth();
        boolean yours = hasYours();
        drawLegend(canvas, 0, 0, new String[]{className, yours ? yourName : null},
                new int[]{SECOND_SERIES, cPrimary});
        float valueW = dp(42);
        float barArea = w - valueW;
        float barH = dp(10);
        float y = dp(24);
        float rowH = rowHeight();
        for (int i = 0; i < classValues.length; i++) {
            text.setTextSize(sp(12));
            text.setColor(cText);
            text.setTextAlign(Paint.Align.LEFT);
            String label = i < labels.size() ? labels.get(i) : "";
            canvas.drawText(TextUtils.ellipsize(label, text, w, TextUtils.TruncateAt.END).toString(),
                    0, y + sp(12), text);
            float by = y + sp(14) + dp(6);
            drawBar(canvas, classValues[i], SECOND_SERIES, by, barH, barArea);
            if (yours) {
                drawBar(canvas, i < yourValues.length ? yourValues[i] : Double.NaN, cPrimary,
                        by + barH + dp(4), barH, barArea);
            }
            y += rowH;
        }
    }

    private void drawBar(Canvas canvas, double pct, int color, float y, float barH, float barArea) {
        rect.set(0, y, barArea, y + barH);
        fill.setColor(cEmpty);
        canvas.drawRoundRect(rect, barH / 2, barH / 2, fill);
        if (Double.isNaN(pct)) {
            text.setTextSize(sp(10));
            text.setColor(cMuted);
            text.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("-", barArea + dp(6), y + barH - dp(1), text);
            return;
        }
        float len = (float) (Math.max(0, Math.min(100, pct)) / 100.0 * barArea);
        rect.set(0, y, Math.max(len, barH), y + barH);
        fill.setColor(color);
        canvas.drawRoundRect(rect, barH / 2, barH / 2, fill);
        text.setTextSize(sp(11));
        text.setColor(cText);
        text.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(Math.round(pct) + "%", barArea + dp(6), y + barH - dp(0.5f), text);
    }
}
