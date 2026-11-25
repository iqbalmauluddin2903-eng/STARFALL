package starfall.audio;

import starfall.core.GameConfig;
import starfall.utils.Logger;

import javax.sound.sampled.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioManager {

    private static final float SAMPLE_RATE = 22050.0f;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

    private final GameConfig config;
    private final ExecutorService sfxExecutor;
    private Thread musicThread;
    private volatile boolean running = true;
    private volatile boolean inMenuMusic = true;
    private volatile boolean audioHardwareAvailable = true;

    public AudioManager(GameConfig config) {
        this.config = config;
        this.sfxExecutor = Executors.newFixedThreadPool(3, r -> {
            Thread t = new Thread(r, "Starfall-SFX");
            t.setDaemon(true);
            return t;
        });
    }

    public void startMusicLoop() {
        if (musicThread != null && musicThread.isAlive()) return;
        musicThread = new Thread(this::runAmbientMusicLoop, "Starfall-Music");
        musicThread.setDaemon(true);
        musicThread.start();
    }

    public void stop() {
        running = false;
        if (musicThread != null) {
            musicThread.interrupt();
        }
        sfxExecutor.shutdownNow();
    }

    public void setMenuMusicMode(boolean inMenu) {
        this.inMenuMusic = inMenu;
    }

    public void playButtonHover() {
        playToneAsync(680.0, 760.0, 45, 0.14);
    }

    public void playButtonClick() {
        playToneAsync(520.0, 880.0, 85, 0.28);
    }

    public void playFootstep() {
        playToneAsync(135.0, 95.0, 38, 0.12);
    }

    public void playKill() {
        sfxExecutor.submit(() -> {
            playSweep(240.0, 68.0, 260, 0.55);
            playSweep(150.0, 48.0, 220, 0.50);
        });
    }

    public void playVent() {
        playToneAsync(190.0, 340.0, 140, 0.25);
    }

    public void playEmergencyMeeting() {
        sfxExecutor.submit(() -> {
            playSweep(620.0, 620.0, 160, 0.52);
            sleepQuiet(50);
            playSweep(880.0, 880.0, 280, 0.55);
        });
    }

    public void playSabotageAlarm() {
        sfxExecutor.submit(() -> {
            playSweep(540.0, 420.0, 210, 0.45);
            sleepQuiet(60);
            playSweep(540.0, 420.0, 210, 0.45);
        });
    }

    public void playTaskComplete() {
        sfxExecutor.submit(() -> {
            playSweep(523.25, 523.25, 95, 0.35);
            playSweep(659.25, 659.25, 95, 0.35);
            playSweep(783.99, 1046.50, 180, 0.40);
        });
    }

    public void playVictory() {
        sfxExecutor.submit(() -> {
            playSweep(440.0, 440.0, 130, 0.42);
            playSweep(554.37, 554.37, 130, 0.42);
            playSweep(659.25, 659.25, 140, 0.45);
            playSweep(880.0, 880.0, 340, 0.48);
        });
    }

    public void playDefeat() {
        sfxExecutor.submit(() -> {
            playSweep(349.23, 329.63, 220, 0.42);
            playSweep(311.13, 293.66, 220, 0.42);
            playSweep(261.63, 196.00, 420, 0.45);
        });
    }

    private void playToneAsync(double startFreq, double endFreq, int durationMs, double baseGain) {
        if (!running || !audioHardwareAvailable) return;
        float gain = getEffectiveSfxGain(baseGain);
        if (gain <= 0.002f) return;
        sfxExecutor.submit(() -> playSweep(startFreq, endFreq, durationMs, baseGain));
    }

    private void playSweep(double startFreq, double endFreq, int durationMs, double baseGain) {
        if (!running || !audioHardwareAvailable) return;
        float gain = getEffectiveSfxGain(baseGain);
        if (gain <= 0.002f) return;

        int numSamples = (int) ((durationMs / 1000.0) * SAMPLE_RATE);
        byte[] buffer = new byte[numSamples * 2];

        double phase = 0.0;
        for (int i = 0; i < numSamples; i++) {
            double t = i / (double) Math.max(1, numSamples - 1);
            double freq = startFreq + (endFreq - startFreq) * t;
            // Smooth attack/release envelope to prevent audio clicks
            double env = Math.min(1.0, Math.min(i / 120.0, (numSamples - i) / 180.0));
            double sample = Math.sin(phase) * env * gain;
            phase += (2.0 * Math.PI * freq) / SAMPLE_RATE;

            short pcm = (short) Math.max(-32767, Math.min(32767, sample * 32767.0));
            buffer[i * 2] = (byte) (pcm & 0xFF);
            buffer[i * 2 + 1] = (byte) ((pcm >> 8) & 0xFF);
        }

        try (SourceDataLine line = AudioSystem.getSourceDataLine(FORMAT)) {
            line.open(FORMAT, buffer.length);
            line.start();
            line.write(buffer, 0, buffer.length);
            line.drain();
        } catch (LineUnavailableException | IllegalArgumentException e) {
            audioHardwareAvailable = false;
            Logger.warn("Java Sound audio line unavailable: " + e.getMessage());
        }
    }

    private void runAmbientMusicLoop() {
        double[] menuArp = {220.0, 261.63, 329.63, 392.00, 329.63, 261.63};
        double[] shipDrone = {110.0, 110.0, 130.81, 98.00};
        int step = 0;

        while (running) {
            if (!audioHardwareAvailable) {
                sleepQuiet(1000);
                continue;
            }

            float musicGain = config.getMasterVolume() * config.getMusicVolume() * 0.14f;
            if (musicGain <= 0.002f) {
                sleepQuiet(300);
                continue;
            }

            double[] pattern = inMenuMusic ? menuArp : shipDrone;
            double freq = pattern[step % pattern.length];
            int noteMs = inMenuMusic ? 420 : 750;
            step++;

            int numSamples = (int) ((noteMs / 1000.0) * SAMPLE_RATE);
            byte[] buffer = new byte[numSamples * 2];
            double phase = 0.0;

            for (int i = 0; i < numSamples; i++) {
                double env = Math.sin((i / (double) numSamples) * Math.PI);
                double wave = (Math.sin(phase) + 0.35 * Math.sin(phase * 0.5)) * env * musicGain;
                phase += (2.0 * Math.PI * freq) / SAMPLE_RATE;

                short pcm = (short) Math.max(-32767, Math.min(32767, wave * 32767.0));
                buffer[i * 2] = (byte) (pcm & 0xFF);
                buffer[i * 2 + 1] = (byte) ((pcm >> 8) & 0xFF);
            }

            try (SourceDataLine line = AudioSystem.getSourceDataLine(FORMAT)) {
                line.open(FORMAT, buffer.length);
                line.start();
                line.write(buffer, 0, buffer.length);
                line.drain();
            } catch (LineUnavailableException | IllegalArgumentException e) {
                audioHardwareAvailable = false;
                Logger.warn("Music audio line unavailable: " + e.getMessage());
            }
        }
    }

    private float getEffectiveSfxGain(double baseGain) {
        return (float) (config.getMasterVolume() * config.getSfxVolume() * baseGain);
    }

    private void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}