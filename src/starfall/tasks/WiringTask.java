package starfall.tasks;

import starfall.core.InputManager;
import starfall.map.RoomId;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WiringTask extends Task {

    private static final Color[] WIRE_COLORS = {
        new Color(235, 55, 55),
        new Color(55, 130, 245),
        new Color(245, 215, 45),
        new Color(235, 85, 195)
    };

    private final int[] rightOrder = {0, 1, 2, 3};
    private final boolean[] connected = new boolean[4];
    private int selectedLeft = -1;
    private double completeDelay = 0.0;

    public WiringTask(String taskId, String interactableId, RoomId location) {
        super(taskId, interactableId, "Fix Wiring", location,
              "Connect each colored wire on the left to its matching terminal on the right.");
    }

    @Override
    protected void onOpen() {
        List<Integer> list = new ArrayList<>(List.of(0, 1, 2, 3));
        Collections.shuffle(list);
        for (int i = 0; i < 4; i++) {
            rightOrder[i] = list.get(i);
            connected[i] = false;
        }
        selectedLeft = -1;
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

        Rectangle box = getModalBounds(screenW, screenH, 560, 390);
        int mx = input.getMouseX();
        int my = input.getMouseY();

        int leftX = box.x + 65;
        int rightX = box.x + box.width - 65;

        // Check left terminals
        for (int i = 0; i < 4; i++) {
            int ty = box.y + 105 + i * 68;
            if (!connected[i] && Math.hypot(mx - leftX, my - ty) <= 26) {
                selectedLeft = i;
                return;
            }
        }

        // Check right terminals
        if (selectedLeft >= 0) {
            for (int slot = 0; slot < 4; slot++) {
                int ty = box.y + 105 + slot * 68;
                if (Math.hypot(mx - rightX, my - ty) <= 26) {
                    if (rightOrder[slot] == selectedLeft) {
                        connected[selectedLeft] = true;
                        selectedLeft = -1;
                        int count = 0;
                        for (boolean b : connected) if (b) count++;
                        progress = count / 4.0f;
                        if (count == 4) {
                            completeDelay = 0.45;
                        }
                    } else {
                        selectedLeft = -1;
                    }
                    return;
                }
            }
        }
    }

    @Override
    public void render(Graphics2D g2, int screenW, int screenH) {
        Rectangle box = getModalBounds(screenW, screenH, 560, 390);
        drawModalFrame(g2, box, "Click a left wire node, then click the matching color on the right.");

        int leftX = box.x + 65;
        int rightX = box.x + box.width - 65;

        // Draw already connected wires
        g2.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int leftIdx = 0; leftIdx < 4; leftIdx++) {
            if (connected[leftIdx]) {
                int rightSlot = findRightSlot(leftIdx);
                int y1 = box.y + 105 + leftIdx * 68;
                int y2 = box.y + 105 + rightSlot * 68;
                g2.setColor(WIRE_COLORS[leftIdx]);
                g2.drawLine(leftX, y1, rightX, y2);
            }
        }

        // Draw terminals
        for (int i = 0; i < 4; i++) {
            int y = box.y + 105 + i * 68;

            // Left node
            g2.setColor(WIRE_COLORS[i]);
            g2.fillRoundRect(leftX - 24, y - 14, 48, 28, 8, 8);
            g2.setColor(selectedLeft == i ? Color.WHITE : Color.BLACK);
            g2.setStroke(new BasicStroke(selectedLeft == i ? 3f : 2f));
            g2.drawRoundRect(leftX - 24, y - 14, 48, 28, 8, 8);

            // Right node
            int colorIdx = rightOrder[i];
            g2.setColor(WIRE_COLORS[colorIdx]);
            g2.fillRoundRect(rightX - 24, y - 14, 48, 28, 8, 8);
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(rightX - 24, y - 14, 48, 28, 8, 8);
        }

        if (completeDelay > 0) {
            g2.setFont(new Font("SansSerif", Font.BOLD, 24));
            g2.setColor(new Color(80, 250, 140));
            g2.drawString("CIRCUIT RESTORED!", box.x + 165, box.y + box.height - 16);
        }
    }

    private int findRightSlot(int colorIdx) {
        for (int i = 0; i < 4; i++) {
            if (rightOrder[i] == colorIdx) return i;
        }
        return 0;
    }
}