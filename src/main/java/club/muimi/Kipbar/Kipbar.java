package club.muimi.Kipbar;

import java.io.Closeable;
import java.io.PrintStream;
import java.time.Duration;
import java.util.Objects;

import lombok.Getter;

@Getter
public class Kipbar implements Closeable {
    private static final int MAX_LENGTH = 100;
    private static final String DEFAULT_SUCCESS_MESSAGE = "Done!";
    private static final KipbarOptions DEFAULT_OPTIONS = KipbarOptions.builder().build();

    private final int length;
    private final char progressChar;
    private final boolean printDoneMessage;
    private final boolean showPercentage;
    private final boolean showElapsedTime;
    private final PrintStream output;
    private final String taskName;
    private final String failureMessage;

    private int progress;
    private KipbarState state = KipbarState.READY;
    private boolean closed;
    private int lastRenderLength;
    private long startNanos;
    private long finishedNanos;

    public Kipbar(int length) {
        this(length, DEFAULT_OPTIONS);
    }

    public Kipbar(int length, char progressChar) {
        this(length, KipbarOptions.builder().progressChar(progressChar).build());
    }

    public Kipbar(int length, boolean printDoneMessage) {
        this(length, KipbarOptions.builder().printDoneMessage(printDoneMessage).build());
    }

    public Kipbar(int length, char progressChar, boolean printDoneMessage) {
        this(length, KipbarOptions.builder()
                .progressChar(progressChar)
                .printDoneMessage(printDoneMessage)
                .build());
    }

    public Kipbar(int length, KipbarOptions options) {
        validateLength(length);
        Objects.requireNonNull(options, "options must not be null");
        this.length = length;
        this.progressChar = options.getProgressChar();
        this.printDoneMessage = options.isPrintDoneMessage();
        this.showPercentage = options.isShowPercentage();
        this.showElapsedTime = options.isShowElapsedTime();
        this.output = Objects.requireNonNull(options.getOutput(), "output must not be null");
        this.taskName = normalizeTaskName(options.getTaskName());
        this.failureMessage = normalizeFailureMessage(options.getFailureMessage());
    }

    public void run(InterruptibleTask task) throws InterruptedException {
        if (task == null) {
            throw new IllegalArgumentException("task must not be null");
        }
        ensureOpen();
        start();
        boolean completed = false;
        try {
            task.run(this);
            completed = true;
        } finally {
            if (state == KipbarState.RUNNING) {
                complete(completed);
            }
        }
    }

    public void start() {
        ensureStartable();
        progress = 0;
        state = KipbarState.RUNNING;
        lastRenderLength = 0;
        startNanos = System.nanoTime();
        finishedNanos = 0L;
        render();
    }

    public void update(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("update value must be non-negative");
        }
        ensureActive();
        progress = Math.min(length, progress + n);
        render();
    }

    public void step() {
        update(1);
    }

    public void finish() {
        complete(true);
    }

    public void fail() {
        complete(false);
    }

    public boolean isStarted() {
        return state != KipbarState.READY;
    }

    public boolean isFinished() {
        return state.isTerminal();
    }

    public Duration getElapsed() {
        if (!isStarted()) {
            return Duration.ZERO;
        }
        long endNanos = state.isTerminal() ? finishedNanos : System.nanoTime();
        return Duration.ofNanos(endNanos - startNanos);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        if (state == KipbarState.RUNNING) {
            complete(false);
        }
        closed = true;
    }

    private void complete(boolean success) {
        ensureOpen();
        ensureStarted();
        if (state.isTerminal()) {
            return;
        }
        if (success) {
            progress = length;
        }
        state = success ? KipbarState.SUCCEEDED : KipbarState.FAILED;
        finishedNanos = System.nanoTime();
        render();
        output.println();
        printCompletionMessage(success);
    }

    private void render() {
        String line = buildLine();
        int padding = Math.max(0, lastRenderLength - line.length());
        output.print(line);
        if (padding > 0) {
            output.print(" ".repeat(padding));
        }
        output.flush();
        lastRenderLength = line.length();
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("progress bar has already been closed");
        }
    }

    private void ensureActive() {
        ensureOpen();
        ensureStarted();
        ensureNotFinished();
    }

    private void ensureStartable() {
        ensureOpen();
        if (state == KipbarState.RUNNING) {
            throw new IllegalStateException("progress bar has already started");
        }
        if (state.isTerminal()) {
            throw new IllegalStateException("progress bar has already finished");
        }
    }

    private void ensureNotFinished() {
        if (state.isTerminal()) {
            throw new IllegalStateException("progress bar has already finished");
        }
    }

    private void ensureStarted() {
        if (!isStarted()) {
            throw new IllegalStateException("progress bar has not started");
        }
    }

    private static void validateLength(int length) {
        if (length <= 0 || length > MAX_LENGTH) {
            throw new IllegalArgumentException("length must be between 1 and " + MAX_LENGTH);
        }
    }

    private String buildLine() {
        int remaining = length - progress;
        StringBuilder builder = new StringBuilder();
        builder.append('\r');
        if (taskName != null) {
            builder.append(taskName).append(' ');
        }
        builder.append('[');
        builder.repeat(String.valueOf(progressChar), progress);
        builder.repeat(" ", remaining);
        builder.append(']');
        if (showPercentage) {
            builder.append(' ').append(String.format("%3d%%", progress * 100 / length));
        }
        if (showElapsedTime) {
            builder.append(' ').append(formatElapsed(getElapsed()));
        }
        return builder.toString();
    }

    private void printCompletionMessage(boolean success) {
        if (success) {
            if (printDoneMessage) {
                output.println(DEFAULT_SUCCESS_MESSAGE);
            }
            return;
        }
        if (failureMessage != null) {
            output.println(failureMessage);
        }
    }

    private static String normalizeTaskName(String taskName) {
        return normalizeText(taskName);
    }

    private static String normalizeFailureMessage(String failureMessage) {
        return normalizeText(failureMessage);
    }

    private static String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String formatElapsed(Duration elapsed) {
        long totalSeconds = elapsed.toSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return "%02d:%02d:%02d".formatted(hours, minutes, seconds);
        }
        return "%02d:%02d".formatted(minutes, seconds);
    }

    @FunctionalInterface
    public interface InterruptibleTask {
        void run(Kipbar bar) throws InterruptedException;
    }

}
