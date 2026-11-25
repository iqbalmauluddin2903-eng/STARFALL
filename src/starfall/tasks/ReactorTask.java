package starfall.tasks;

import starfall.core.InputManager;
import starfall.map.RoomId;

import java.awt.*;
import java.util.Random;

public class ReactorTask extends Task {

    private static final int MAX_STEPS = 4;
    private final int[] sequence = new int[MAX_STEPS];
    private final Random rng = new Random();

    private int currentStepCount = 1;
    private int playIndex = 0;
    private int inputIndex = 0;
    private boolean showingSequence = true;
    private double stepTimer = 0.0;
    private int litButton = -1;

    public ReactorTask(String taskId, String interactableId, RoomId location) {
        super(taskId, interactableId, "Start Reactor", location,
              "Repeat the glowing 3x3 reactor keypad sequence.");
    }

    @Override
    protected void onOpen() {
        for (int i = 0; i < MAX_STEPS; i++) {
            sequence[i] = rng.nextInt(9);
        }
        currentStepCount = 1;
        startPlayback();
        progress = 0.0f;
    }

    private void startPlayback() {
        showingSequence = true;
        playIndex = 0;
        inputIndex = 0;
        stepTimer = 0.45;
        litButton = -1;
    }

    @Override
    public void update(double dt, InputManager input, int screenW, int screenH) {
        if (showingSequence) {
            stepTimer -= dt;
            if (stepTimer <= 0.0) {
                if (litButton != -1) {
                    litButton = -1;
                    playIndex++;
                    stepTimer = 0.18;
                    if (playIndex >= currentStepCount) {
                        showingSequence = false;
                    }
                } else {
                    litButton = sequence[playIndex];
                    stepTimer = 0.42;
                }
            }
            return;
        }

        if (!input.consumeLeftClick()) return;

        Rectangle box = getModalBounds(screenW, screenH, 460, 410);
        int gridX = box.x + 125;
        int gridY = box.y + 115;

        for (int i = 0; i < 9; i++) {
            int col = i % 3;
            int row = i / 3;
            Rectangle cell = new Rectangle(gridX + col * 72, gridY + row * 72, 62, 62);
            if (cell.contains(input.getMouseX(), input.getMouseY())) {
                if (i == sequence[inputIndex]) {
                    litButton = i;
                    inputIndex++;
                    if (inputIndex >= currentStepCount) {
                        progress = currentStepCount / (float) MAX_STEPS;
                        if (currentStepCount >= MAX_STEPS) {
                            markCompleted();
                        } else {
                            currentStepCount++;
                            startPlayback();
                        }
                    }
                } else {
                    // Wrong pad resets current sequence playback
                    startPlayback();
                }
                return;
            }
        }
    }

    @Override
    public void render(Graphics2D g2, int screenW, int screenH) {
        Rectangle box = getModalBounds(screenW, screenH, 460, 410);
        String status = showingSequence ? "WATCH THE PATTERN..." : "YOUR TURN (" + inputIndex + "/" + currentStepCount + ")";
        drawModalFrame(g2, box, status);

        // Stage indicator LEDs
        for (int s = 0; s < MAX_STEPS; s++) {
            g2.setColor(s < currentStepCount - (showingSequence ? 1 : 0) ? new Color(75, 245, 130) : Color.DARK_GRAY);
            g2.fillOval(box.x + 165 + s * 34, box.y + 78, 16, 16);
        }

        int gridX = box.x + 125;
        int gridY = box.y + 115;
        for (int i = 0; i < 9; i++) {
            int col = i % 3;
            int row = i / 3;
            int cx = gridX + col * 72;
            int cy = gridY + row * 72;

            g2.setColor(litButton == i ? new Color(0, 230, 255) : new Color(46, 56, 76));
            g2.fillRoundRect(cx, cy, 62, 62, 10, 10);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(cx, cy, 62, 62, 10, 10);
        }
    }
}