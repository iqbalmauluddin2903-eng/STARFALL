package starfall.tasks;

import starfall.core.InputManager;
import starfall.map.RoomId;

import java.awt.*;

public class DownloadTask extends Task {

    private boolean transferring = false;
    private double completeDelay = 0.0;

    public DownloadTask(String taskId, String interactableId, String name, RoomId location) {
        super(taskId, interactableId, name, location, "Transfer encrypted ship telemetry logs.");
    }

    @Override
    protected void onOpen() {
        transferring = false;
        progress = 0.0f;
        completeDelay = 0.0;
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

        Rectangle box = getModalBounds(screenW, screenH, 520, 310);
        Rectangle btnRect = new Rectangle(box.x + 170, box.y + 215, 180, 44);

        if (!transferring && input.consumeLeftClick()) {
            if (btnRect.contains(input.getMouseX(), input.getMouseY())) {
                transferring = true;
            }
        }

        if (transferring) {
            progress = Math.min(1.0f, progress + (float) (dt * 0.24));
            if (progress >= 1.0f) {
                transferring = false;
                completeDelay = 0.45;
            }
        }
    }

    @Override
    public void render(Graphics2D g2, int screenW, int screenH) {
        Rectangle box = getModalBounds(screenW, screenH, 520, 310);
        drawModalFrame(g2, box, "Click START TRANSFER and wait for packets to reach 100%.");

        // Folder icons
        g2.setColor(new Color(65, 145, 235));
        g2.fillRoundRect(box.x + 75, box.y + 92, 74, 54, 10, 10);
        g2.fillRoundRect(box.x + box.width - 149, box.y + 92, 74, 54, 10, 10);

        g2.setFont(new Font("Consolas", Font.BOLD, 12));
        g2.setColor(Color.WHITE);
        g2.drawString("SHIP CORE", box.x + 79, box.y + 124);
        g2.drawString("DATAPAD", box.x + box.width - 138, box.y + 124);

        // Animated packet dots
        if (transferring) {
            int packetX = box.x + 165 + (int) (((System.currentTimeMillis() % 800) / 800.0) * 185);
            g2.setColor(new Color(80, 245, 160));
            g2.fillRect(packetX, box.y + 112, 16, 12);
        }

        // Progress Bar
        int barX = box.x + 60;
        int barY = box.y + 166;
        int barW = box.width - 120;
        g2.setColor(new Color(35, 42, 58));
        g2.fillRoundRect(barX, barY, barW, 28, 8, 8);
        g2.setColor(new Color(46, 213, 115));
        g2.fillRoundRect(barX, barY, (int) (barW * progress), 28, 8, 8);
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(barX, barY, barW, 28, 8, 8);

        String pctText = String.format("%d%% COMPLETE", (int) (progress * 100));
        g2.drawString(pctText, barX + barW / 2 - 45, barY + 19);

        // Start Button
        Rectangle btnRect = new Rectangle(box.x + 170, box.y + 215, 180, 44);
        g2.setColor(transferring ? Color.GRAY : (progress >= 1.0f ? new Color(46, 213, 115) : new Color(0, 195, 255)));
        g2.fillRoundRect(btnRect.x, btnRect.y, btnRect.width, btnRect.height, 10, 10);
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Consolas", Font.BOLD, 14));
        String btnLabel = progress >= 1.0f ? "DONE!" : (transferring ? "TRANSFERRING..." : "START TRANSFER");
        g2.drawString(btnLabel, btnRect.x + 22, btnRect.y + 27);
    }
}