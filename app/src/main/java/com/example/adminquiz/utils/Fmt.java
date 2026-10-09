package com.example.adminquiz.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class Fmt {
    private Fmt() {
    }

    public static String time(long ms) {
        long s = Math.max(0, ms) / 1000;
        return String.format(Locale.US, "%02d:%02d", s / 60, s % 60);
    }

    public static String seconds(long ms) {
        return (Math.max(0, ms) / 1000) + " sec";
    }

    public static String date(long t) {
        return new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date(t));
    }

    public static int pct(double v) {
        return (int) Math.round(Math.max(0, Math.min(1, v)) * 100);
    }
}
