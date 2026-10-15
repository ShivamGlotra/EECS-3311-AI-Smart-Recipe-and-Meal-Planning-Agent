package com.smartplate.presentation.gui;

import javax.swing.SwingWorker;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Runs AI requests. In the app they run on a background thread (SwingWorker) so the window
 * never freezes while the model answers; tests use {@link #direct()} to run them in place.
 */
public interface TaskRunner {

    <T> void run(Callable<T> task, Consumer<T> onSuccess, Consumer<RuntimeException> onFailure);

    /** Background thread, results delivered on the Swing event thread. */
    static TaskRunner background() {
        return new TaskRunner() {
            @Override
            public <T> void run(Callable<T> task, Consumer<T> onSuccess, Consumer<RuntimeException> onFailure) {
                new SwingWorker<T, Void>() {
                    @Override
                    protected T doInBackground() throws Exception {
                        return task.call();
                    }

                    @Override
                    protected void done() {
                        try {
                            onSuccess.accept(get());
                        } catch (ExecutionException e) {
                            Throwable cause = e.getCause();
                            onFailure.accept(cause instanceof RuntimeException
                                    ? (RuntimeException) cause : new RuntimeException(cause));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }.execute();
            }
        };
    }

    /** Runs the task immediately on the calling thread. */
    static TaskRunner direct() {
        return new TaskRunner() {
            @Override
            public <T> void run(Callable<T> task, Consumer<T> onSuccess, Consumer<RuntimeException> onFailure) {
                T result;
                try {
                    result = task.call();
                } catch (RuntimeException e) {
                    onFailure.accept(e);
                    return;
                } catch (Exception e) {
                    onFailure.accept(new RuntimeException(e));
                    return;
                }
                onSuccess.accept(result);
            }
        };
    }
}
