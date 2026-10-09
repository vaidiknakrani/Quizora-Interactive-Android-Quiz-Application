package com.example.adminquiz.utils;

import java.util.Locale;
import java.util.regex.Pattern;

/** Class identifier typed by the student, e.g. "cs-2a". Stored upper-case; blank means GENERAL. */
public final class ClassCode {
    public static final String DEFAULT = "GENERAL";
    private static final Pattern VALID = Pattern.compile("[A-Z0-9_-]{2,20}");

    private ClassCode() {
    }

    /** Returns the normalised code, DEFAULT for blank input, or null when the input is invalid. */
    public static String normalize(String raw) {
        if (raw == null) {
            return DEFAULT;
        }
        String s = raw.trim().toUpperCase(Locale.US);
        if (s.isEmpty()) {
            return DEFAULT;
        }
        return VALID.matcher(s).matches() ? s : null;
    }

    /** Code stored on a user row; rows created before classes existed count as DEFAULT. */
    public static String of(String stored) {
        return stored == null || stored.trim().isEmpty() ? DEFAULT : stored;
    }
}
