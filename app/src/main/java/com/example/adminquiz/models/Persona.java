package com.example.adminquiz.models;

import android.graphics.Color;

public enum Persona {
    SPEED_DEMON("THE SPEED DEMON", "\u26A1", "Fast brain. Faster answers.", "#FF9800"),
    KNOWLEDGE_MACHINE("THE KNOWLEDGE MACHINE", "\uD83E\uDDE0", "Facts in. Answers out.", "#3F51B5"),
    STRATEGIST("THE STRATEGIST", "\u265F\uFE0F", "Every answer is a calculated move.", "#009688"),
    DEEP_THINKER("THE DEEP THINKER", "\uD83E\uDD14", "Slow and sure wins the quiz.", "#795548"),
    PERFECTIONIST("THE PERFECTIONIST", "\uD83D\uDC8E", "Flawless. Absolutely flawless.", "#00ACC1"),
    RISK_TAKER("THE RISK TAKER", "\uD83C\uDFB2", "Fortune favors the bold.", "#E91E63"),
    COMEBACK_KING("THE COMEBACK KING", "\uD83D\uDC51", "Down early. Dominant late.", "#9C27B0"),
    LUCKY_GENIUS("THE LUCKY GENIUS", "\uD83C\uDF40", "Brilliance or luck? Who knows.", "#43A047");

    public final String title;
    public final String emoji;
    public final String tagline;
    public final String colorHex;

    Persona(String title, String emoji, String tagline, String colorHex) {
        this.title = title;
        this.emoji = emoji;
        this.tagline = tagline;
        this.colorHex = colorHex;
    }

    public int color() {
        return Color.parseColor(colorHex);
    }

    public static Persona fromName(String name) {
        try {
            return valueOf(name);
        } catch (Exception e) {
            return STRATEGIST;
        }
    }
}
