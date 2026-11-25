package starfall.core;

import starfall.utils.Logger;

import java.util.function.DoubleConsumer;

public class GameLoop implements Runnable {

    private final int targetFps;
    private final DoubleConsumer updateCallback;
    private final Runnable renderCallback;

    private volatile boolean running = false;
    private Thread loopThread;

    private volatile int currentFps = 0;
    private volatile double lastDeltaSeconds = 0.0;

    public GameLoop(int targetFps, DoubleConsumer updateCallback, Runnable renderCallback) {
        this.targetFps = Math.max(30, targetFps);
        this.updateCallback = updateCallback;
        this.renderCallback = renderCallback;
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        loopThread = new Thread(this, "Starfall-GameLoop");
        loopThread.setDaemon(true);
        loopThread.start();
        Logger.info("GameLoop started (Target FPS: " + targetFps + ")");
    }

    public synchronized void stop() {
        if (!running) return;
        running = false;
        if (loopThread != null) {
            loopThread.interrupt();
        }
        Logger.info("GameLoop stopped.");
    }

    public int getCurrentFps() { return currentFps; }
    public double getLastDeltaSeconds() { return lastDeltaSeconds; }

    @Override
    public void run() {
        final long optimalFrameTimeNs = 1_000_000_000L / targetFps;
        long lastTime = System.nanoTime();
        long fpsTimer = System.currentTimeMillis();
        int frames = 0;

        while (running) {
            long now = System.nanoTime();
            long elapsedNs = now - lastTime;
            lastTime = now;

            // Clamp delta time to prevent physics jumps after window drags
            double dt = Math.min(0.05, elapsedNs / 1_000_000_000.0);
            lastDeltaSeconds = dt;

            try {
                updateCallback.accept(dt);
                renderCallback.run();
            } catch (Exception ex) {
                Logger.error("Exception in GameLoop tick", ex);
            }

            frames++;
            if (System.currentTimeMillis() - fpsTimer >= 1000) {
                currentFps = frames;
                frames = 0;
                fpsTimer += 1000;
            }

            long frameDurationNs = System.nanoTime() - now;
            long sleepTimeMs = (optimalFrameTimeNs - frameDurationNs) / 1_000_000L;
            if (sleepTimeMs > 0) {
                try {
                    Thread.sleep(sleepTimeMs);
                } catch (InterruptedException e) {
                    if (!running) break;
                }
            }
        }
    }
}