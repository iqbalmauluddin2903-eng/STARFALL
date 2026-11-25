package starfall.tasks;

import starfall.core.InputManager;
import starfall.map.RoomId;

import java.awt.*;

public class CardTask extends Task {

    private double cardX = 0.0;
    private boolean dragging = false;
    private long dragStartMs = 0L;
    private String feedback = "Drag the ID card smoothly from left to right.";
    private Color feedbackColor = Color.LIGHT_GRAY;
    private double completeDelay = 0.0;

    public CardTask(String taskId, String interactableId, RoomId location) {
        super(taskId, interactableId, "Swipe Card", location, "Swipe your Crew ID card through the Admin reader.");
    }

    @Override
    protected void onOpen() {
        cardX = 0.0;
        dragging = false;
        feedback = "Hold Left Mouse and drag card right (~0.8s).";
        feedbackColor = new Color(180, 220, 255);
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

        Rectangle box = getModalBounds(screenW, screenH, 560, 320);
        int trackStart = box.x + 60;
        int trackEnd = box.x + box.width - 170;

        int mx = input.getMouseX();
        int my = input.getMouseY();

        if (input.isLeftMouseDown()) {
            Rectangle cardRect = new Rectangle((int) (trackStart + cardX), box.y + 175, 115, 72);
            if (!dragging && cardRect.contains(mx, my)) {
                dragging = true;
                dragStartMs = System.currentTimeMillis();
                feedback = "SWIPING...";
                feedbackColor = new Color(255, 225, 80);
            }
            if (dragging) {
                cardX = Math.max(0.0, Math.min(trackEnd - trackStart, mx - trackStart - 55));
                progress = (float) (cardX / (trackEnd - trackStart));

                if (cardX >= (trackEnd - trackStart) - 4) {
                    dragging = false;
                    long duration = System.currentTimeMillis() - dragStartMs;
                    if (duration < 380) {
                        feedback = "TOO FAST! Try a steadier swipe.";
                        feedbackColor = new Color(255, 85, 85);
                        cardX = 0.0;
                        progress = 0.0f;
                    } else if (duration > 1800) {
                        feedback = "TOO SLOW! Swipe a bit faster.";
                        feedbackColor = new Color(255, 85, 85);
                        cardX = 0.0;
                        progress = 0.0f;
                    } else {
                        feedback = "ACCEPTED! Identity Verified.";
                        feedbackColor = new Color(75, 245, 140);
                        progress = 1.0f;
                        completeDelay = 0.45;
                    }
                }
            }
        } else if (dragging) {
            dragging = false;
            feedback = "BAD READ! Complete the full swipe.";
            feedbackColor = new Color(255, 150, 65);
            cardX = 0.0;
            progress = 0.0f;
        }
    }

    @Override
    public void render(Graphics2D g2, int screenW, int screenH) {
        Rectangle box = getModalBounds(screenW, screenH, 560, 320);
        drawModalFrame(g2, box, "Drag the ID card smoothly through the magnetic reader.");

        // Card Reader Top Hood
        g2.setColor(new Color(45, 52, 68));
        g2.fillRoundRect(box.x + 45, box.y + 80, box.width - 90, 68, 12, 12);
        g2.setColor(new Color(20, 25, 34));
        g2.fillRect(box.x + 55, box.y + 124, box.width - 110, 12);

        g2.setFont(new Font("Consolas", Font.BOLD, 14));
        g2.setColor(feedbackColor);
        g2.drawString(feedback, box.x + 65, box.y + 110);

        // Movable ID Card
        int cx = (int) (box.x + 60 + cardX);
        int cy = box.y + 175;
        g2.setColor(new Color(235, 242, 252));
        g2.fillRoundRect(cx, cy, 115, 72, 10, 10);
        g2.setColor(new Color(0, 185, 235));
        g2.fillRect(cx + 10, cy + 14, 28, 32);
        g2.setColor(new Color(50, 60, 80));
        g2.fillRect(cx + 46, cy + 16, 56, 8);
        g2.fillRect(cx + 46, cy + 32, 42, 8);
        g2.setColor(new Color(220, 175, 45));
        g2.fillRect(cx, cy + 54, 115, 10);
    }
}