package com.template.util;

import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Runs slow work (database calls) off the JavaFX thread so the window never freezes. */
public final class Async {

    private Async() {}

    public static <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            ex.printStackTrace();
            if (onError != null) onError.accept(ex);
        });
        Thread t = new Thread(task, "db-worker");
        t.setDaemon(true);
        t.start();
    }
}