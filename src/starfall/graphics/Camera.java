package starfall.graphics;

import starfall.map.GameMap;

public class Camera {

    private double x;
    private double y;
    private int viewportWidth;
    private int viewportHeight;
    private double smoothSpeed = 8.5;

    public Camera(int viewportWidth, int viewportHeight) {
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
    }

    public void snapTo(double targetX, double targetY) {
        this.x = clampX(targetX - viewportWidth / 2.0);
        this.y = clampY(targetY - viewportHeight / 2.0);
    }

    public void follow(double targetX, double targetY, double dt) {
        double desiredX = clampX(targetX - viewportWidth / 2.0);
        double desiredY = clampY(targetY - viewportHeight / 2.0);

        double factor = Math.min(1.0, dt * smoothSpeed);
        this.x += (desiredX - this.x) * factor;
        this.y += (desiredY - this.y) * factor;
    }

    public void setViewportSize(int width, int height) {
        this.viewportWidth = width;
        this.viewportHeight = height;
    }

    private double clampX(double val) {
        double max = Math.max(0, GameMap.WORLD_WIDTH - viewportWidth);
        return Math.max(0, Math.min(max, val));
    }

    private double clampY(double val) {
        double max = Math.max(0, GameMap.WORLD_HEIGHT - viewportHeight);
        return Math.max(0, Math.min(max, val));
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public int getViewportWidth() { return viewportWidth; }
    public int getViewportHeight() { return viewportHeight; }
}