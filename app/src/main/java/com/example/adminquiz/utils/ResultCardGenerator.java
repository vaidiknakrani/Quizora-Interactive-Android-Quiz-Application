package com.example.adminquiz.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.example.adminquiz.models.Persona;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Draws the shareable persona card locally with Canvas (no network, no external service). */
public final class ResultCardGenerator {
    private ResultCardGenerator() {
    }

    public static Bitmap create(Persona persona, int overall, int accuracy, int speed, int consistency) {
        int w = 1080;
        int h = 1350;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);

        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setShader(new LinearGradient(0, 0, w, h, persona.color(), Color.parseColor("#14001F"),
                Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, bg);

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.WHITE);
        p.setTextAlign(Paint.Align.CENTER);

        p.setTextSize(48);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        p.setLetterSpacing(0.3f);
        c.drawText("QUIZ APP", w / 2f, 120, p);
        p.setLetterSpacing(0f);

        p.setTextSize(260);
        c.drawText(persona.emoji, w / 2f, 480, p);

        p.setTextSize(68);
        c.drawText(persona.title, w / 2f, 640, p);

        p.setTextSize(42);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        c.drawText("\u201C" + persona.tagline + "\u201D", w / 2f, 720, p);

        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        p.setTextSize(120);
        c.drawText("Score: " + overall + "%", w / 2f, 920, p);

        p.setTypeface(Typeface.DEFAULT);
        p.setTextSize(56);
        c.drawText("Accuracy: " + accuracy + "%", w / 2f, 1040, p);
        c.drawText("Speed: " + speed + "%", w / 2f, 1120, p);
        c.drawText("Consistency: " + consistency + "%", w / 2f, 1200, p);
        return bmp;
    }

    public static void share(Context context, Bitmap bitmap, String text) throws IOException {
        File dir = new File(context.getCacheDir(), "shared");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Cannot create cache dir");
        }
        File file = new File(dir, "result_card.png");
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/png");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, "Share result"));
    }
}
