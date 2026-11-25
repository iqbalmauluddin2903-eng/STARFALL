package starfall.tasks;

import starfall.core.InputManager;
import starfall.map.RoomId;

import java.awt.*;
import java.util.Random;

public class CalibrationTask extends Task {

    private final boolean[] nodesDone = new boolean[4];
    private final int[] targetValues = new int[4];
    private final Random rng = new Random();
    private double completeDelay = 0.0;

    public CalibrationTask(String taskId, String interactableId, String name, RoomId location, String description) {
        super(taskId, interactableId, name, location, description);
    }

    @Override
    protected void onOpen() {
        for (int i = 0; i < 4; i++) {
            nodesDone[i] = false;
            targetValues[i] = 25 + rng.nextInt(70);
        }
        completeDelay = 0.0;
        progress = 0.0f;
    }

    @Override
    public void update(double dt, InputManager input, int screenW, int screenH) {
        if (completeDelay > 0.0) {
            completeDelay -= dt;
            if (completeDelay <= 0.0) {
                markCompleted();
            }
            return;
        }

        if (!input.consumeLeftClick()) return;

        Rectangle box = getModalBounds(screenW, screenH, 520, 350);
        for (int i = 0; i < 4; i++) {
            int rowY = box.y + 92 + i * 56;
            Rectangle btn = new Rectangle(box.x + box.width - 145, rowY, 110, 38);
            if (!nodesDone[i] && btn.contains(input.getMouseX(), input.getMouseY())) {
                nodesDone[i] = true;
                int doneCount = 0;
                for (boolean b : nodesDone) if (b) doneCount++;
                progress = doneCount / 4.0f;
                if (doneCount == 4) {
                    completeDelay = 0.4;
                }
                return;
            }
        }
    }

    @Override
    public void render(Graphics2D g2, int screenW, int screenH) {
        Rectangle box = getModalBounds(screenW, screenH, 520, 350);
        drawModalFrame(g2, box, description);

        for (int i = 0; i < 4; i++) {
            int rowY = box.y + 92 + i * 56;
            g2.setColor(new Color(34, 42, 60));
            g2.fillRoundRect(box.x + 32, rowY, box.width - 64, 38, 8, 8);

            g2.setFont(new Font("Consolas", Font.BOLD, 13));
            g2.setColor(Color.WHITE);
            g2.drawString("MODULE 0" + (i + 1) + " [FREQ: " + targetValues[i] + " Hz]", box.x + 48, rowY + 24);

            Rectangle btn = new Rectangle(box.x + box.width - 145, rowY, 110, 38);
            g2.setColor(nodesDone[i] ? new Color(46, 213, 115) : new Color(245, 195, 45));
            g2.fillRoundRect(btn.x, btn.y, btn.width, btn.height, 8, 8);

            g2.setColor(Color.BLACK);
            g2.drawString(nodesDone[i] ? "ALIGNED" : "CALIBRATE", btn.x + 18, btn.y + 24);
        }
    }
}