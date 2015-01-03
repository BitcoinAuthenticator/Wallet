package org.wallet.utils;

import javafx.application.Platform;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A simple wrapper around {@link javafx.application.Platform#runLater(Runnable)} which will do nothing if the previous
 * invocation of runLater didn't execute on the JavaFX UI thread yet. In this way you can avoid flooding
 * the event loop if you have a background thread that for whatever reason wants to update the UI very
 * frequently. Without this class you could end up bloating up memory usage and causing the UI to stutter
 * if the UI thread couldn't keep up with your background worker.
 */
public class ThrottledRunLater implements Runnable {
    private final Runnable runnable;
    private final AtomicBoolean pending = new AtomicBoolean();

    /** Created this way, the no-args runLater will execute this classes run method. */
    public ThrottledRunLater() {
        this.runnable = null;
    }

    /** Created this way, the no-args runLater will execute the given runnable. */
    public ThrottledRunLater(Runnable runnable) {
        this.runnable = runnable;
    }

    public void runLater(Runnable runnable) {
        if (!pending.getAndSet(true)) {
            Platform.runLater(() -> {
                pending.set(false);
                runnable.run();
            });
        }
    }

    public void runLater() {
        runLater(runnable != null ? runnable : this);
    }

    @Override
    public void run() {
    }
}
