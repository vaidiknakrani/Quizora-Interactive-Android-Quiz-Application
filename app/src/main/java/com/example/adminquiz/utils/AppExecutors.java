package com.example.adminquiz.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Background executor. Database work never runs on the main thread. */
public final class AppExecutors {
    private static final ExecutorService IO = Executors.newFixedThreadPool(3);

    private AppExecutors() {
    }

    public static void io(Runnable task) {
        IO.execute(task);
    }
}
