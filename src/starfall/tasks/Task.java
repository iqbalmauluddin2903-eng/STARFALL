package starfall.tasks;

import starfall.core.InputManager;
import starfall.map.RoomId;

import java.awt.*;

public abstract class Task {

    protected final String taskId;
    protected final String interactableId;
    protected final String name;
    protected final RoomId location;
    protected final String description;

    protected boolean completed = false;
    protected boolean active = false;
    protected float progress = 0.0f;

    protected Task(String taskId, String interactableId, String name, RoomId location, String description) {
        this.taskId = taskId;
        this.interactableId = interactableId;
        this.name = name;
        this.location = location;
        this.description = description;
    }

    public void open() {
        if (!completed) {
            this.active = true;
            onOpen();
        }
    }

    public void close() {
        this.active = false;
    }

    protected void markCompleted() {
        this.completed = true;
        this.progress = 1.0f;
        this.active = false;
    }

    protected abstract void onOpen();

    public abstract void update(double dt, InputManager input, int screenW, int screenH);

    public abstract void render(Graphics2D g2, int screenW, int screenH);

    protected Rectangle getModalBounds(int screenW, int screenH, int width, int height) {
        return new Rectangle((screenW - width) / 2, (screenH - height) / 2, width, height);
    }

    protected void drawModalFrame(Graphics2D g2, Rectangle box, String subtitle) {
        // Dim background
        g2.setColor(new Color(5, 8, 15, 175));
        g2.fillRect(0, 0, 4000, 3000);

        // Panel body
        g2.setColor(new Color(22, 28, 42));
        g2.fillRoundRect(box.x, box.y, box.width, box.height, 18, 18);
        g2.setColor(new Color(0, 215, 255));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawRoundRect(box.x, box.y, box.width, box.height, 18, 18);

        // Header bar
        g2.setColor(new Color(14, 18, 28));
        g2.fillRoundRect(box.x + 4, box.y + 4, box.width - 8, 54, 14, 14);

        g2.setFont(new Font("SansSerif", Font.BOLD, 20));
        g2.setColor(Color.WHITE);
        g2.drawString(name.toUpperCase() + " — " + location.getDisplayName(), box.x + 20, box.y + 30);

        g2.setFont(new Font("Consolas", Font.PLAIN, 12));
        g2.setColor(new Color(160, 210, 245));
        g2.drawString(subtitle, box.x + 20, box.y + 49);

        // Close hint [ESC]
        g2.setColor(new Color(255, 100, 100));
        g2.setFont(new Font("Consolas", Font.BOLD, 13));
        g2.drawString("[ESC] CLOSE", box.x + box.width - 105, box.y + 32);
    }

    public String getTaskId() { return taskId; }
    public String getInteractableId() { return interactableId; }
    public String getName() { return name; }
    public RoomId getLocation() { return location; }
    public String getDescription() { return description; }
    public boolean isCompleted() { return completed; }
    public boolean isActive() { return active; }
    public float getProgress() { return progress; }
}