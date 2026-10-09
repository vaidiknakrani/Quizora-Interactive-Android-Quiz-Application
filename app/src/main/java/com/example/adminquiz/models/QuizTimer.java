package com.example.adminquiz.models;

import android.os.Handler;
import android.os.Looper;

/** Ticks twice a second so the UI can refresh the clock and detect time-up. */
public class QuizTimer {

    public interface Listener {
        void onTick();

        void onTimeUp();
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final QuizSession session;
    private final Listener listener;
    private boolean running;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            listener.onTick();
            if (session.getTimeLimitMs() > 0 && session.getRemainingMs() <= 0) {
                running = false;
                listener.onTimeUp();
                return;
            }
            handler.postDelayed(this, 500);
        }
    };

    public QuizTimer(QuizSession session, Listener listener) {
        this.session = session;
        this.listener = listener;
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        handler.post(tick);
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(tick);
    }
}
