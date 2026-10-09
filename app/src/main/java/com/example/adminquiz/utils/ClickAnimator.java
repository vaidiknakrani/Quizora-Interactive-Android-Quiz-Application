package com.example.adminquiz.utils;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;

/** Adds a subtle press-down scale animation to any view. */
public final class ClickAnimator {

    private ClickAnimator() {
    }

    @SuppressLint("ClickableViewAccessibility")
    public static void attach(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    break;
                default:
                    break;
            }
            return false; // let the click and ripple proceed
        });
    }
}
