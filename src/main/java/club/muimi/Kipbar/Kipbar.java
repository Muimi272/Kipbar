package club.muimi.Kipbar;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class Kipbar {
    private final int length;
    private int progress = 0;
    private volatile boolean running = false;
    private final BlockingDeque<ProgressEvent> events;
    private char progressChar = '#';
    private boolean requireDone = true;

    public Kipbar(int length) {
        if (length <= 0 || length > 100) throw new IllegalArgumentException("length must be between 1 and 100");
        this.length = length;
        events = new LinkedBlockingDeque<>(length);
    }

    public Kipbar(int length, char progressChar) {
        this(length);
        this.progressChar = progressChar;
    }

    public Kipbar(int length, boolean requireDone) {
        this(length);
        this.requireDone = requireDone;
    }

    public Kipbar(int length, char progressChar, boolean requireDone) {
        this(length, progressChar);
        this.requireDone = requireDone;
    }

    public void run() throws InterruptedException {
        try {
            new Thread(() -> {
                try {
                    progress();
                } catch (InterruptedException e) {
                    throw new RuntimeException();
                }
            }).start();
            do {
                switch (events.poll(1000, TimeUnit.MILLISECONDS)) {
                    case START -> {
                        running = true;
                        System.out.print("[");
                        for (int i = 0; i < length; i++) System.out.print(" ");
                        System.out.print("]");
                        System.out.print("\u001B[" + (length + 1) + "D");
                    }
                    case UPDATE -> System.out.print(progressChar);
                    case DONE -> running = false;
                    case null -> {}
                }
            } while (running);
            if (requireDone) System.out.println("\nDone!");
        } catch (RuntimeException e) {
            running = false;
            throw e;
        }
    }

    public void progress() throws InterruptedException {
        start();
        for (int i = 1; i <= length; i++) {
            update(1);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException _) {
            }
        }
        done();
    }

    public void start() throws InterruptedException {
        progress = 0;
        events.put(ProgressEvent.START);
    }

    public void done() throws InterruptedException {
        events.put(ProgressEvent.DONE);
    }

    public void update(int n) throws InterruptedException {
        if (n + progress > length) return;
        for (int i = 0; i < n; i++) {
            progress++;
            events.put(ProgressEvent.UPDATE);
        }
    }
}
