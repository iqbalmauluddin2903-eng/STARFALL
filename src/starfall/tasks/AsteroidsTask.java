package starfall.tasks;

import starfall.core.InputManager;
import starfall.map.RoomId;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class AsteroidsTask extends Task {

    private static class Asteroid {
        double x, y, vx, vy, radius;
        Asteroid(double x, double y, double vx, double vy, double radius) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.radius = radius;
        }
    }

    private static final int TARGET_KILLS = 10;
    private final List<Asteroid> asteroids = new ArrayList<>();
    private final Random rng = new Random();
    private int destroyed = 0;
    private double spawnTimer = 0.0;
    private int lastLaserX = -1, lastLaserY = -1;
    private double laserFlash = 0.0;

    public AsteroidsTask(String taskId, String interactableId, RoomId location) {
        super(taskId, interactableId, "Clear Asteroids", location,
              "Shoot incoming meteoroids on the Weapons targeting radar.");
    }

    @Override
    protected void onOpen() {
        asteroids.clear();
        destroyed = 0;
        spawnTimer = 0.0;
        laserFlash = 0.0;
        progress = 0.0f;
        for (int i = 0; i < 4; i++) {
            spawnAsteroid();
        }
    }

    private void spawnAsteroid() {
        double y = 90 + rng.nextDouble() * 230;
        double speed = 95 + rng.nextDouble() * 85;
        asteroids.add(new Asteroid(530, y, -speed, (rng.nextDouble() - 0.5) * 40, 18 + rng.nextInt(10)));
    }

    @Override
    public void update(double dt, InputManager input, int screenW, int screenH) {
        Rectangle box = getModalBounds(screenW, screenH, 580, 410);

        if (laserFlash > 0) {
            laserFlash = Math.max(0.0, laserFlash - dt);
        }

        spawnTimer += dt;
        if (spawnTimer >= 0.75 && asteroids.size() < 6) {
            spawnTimer = 0.0;
            spawnAsteroid();
        }

        Iterator<Asteroid> it = asteroids.iterator();
        while (it.hasNext()) {
            Asteroid a = it.next();
            a.x += a.vx * dt;
            a.y += a.vy * dt;
            if (a.x < 30 || a.y < 75 || a.y > 370) {
                it.remove();
            }
        }

        if (input.consumeLeftClick()) {
            int localX = input.getMouseX() - box.x;
            int localY = input.getMouseY() - box.y;
            if (localX >= 20 && localX <= box.width - 20 && localY >= 68 && localY <= box.height - 20) {
                lastLaserX = input.getMouseX();
                lastLaserY = input.getMouseY();
                laserFlash = 0.14;

                Iterator<Asteroid> hitIt = asteroids.iterator();
                while (hitIt.hasNext()) {
                    Asteroid a = hitIt.next();
                    if (Math.hypot(localX - a.x, localY - a.y) <= a.radius + 8) {
                        hitIt.remove();
                        destroyed++;
                        progress = Math.min(1.0f, destroyed / (float) TARGET_KILLS);
                        if (destroyed >= TARGET_KILLS) {
                            markCompleted();
                        }
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void render(Graphics2D g2, int screenW, int screenH) {
        Rectangle box = getModalBounds(screenW, screenH, 580, 410);
        drawModalFrame(g2, box, "Click asteroids to fire twin plasma cannons (" + destroyed + "/" + TARGET_KILLS + ").");

        // Radar screen
        g2.setColor(new Color(10, 28, 22));
        g2.fillRect(box.x + 20, box.y + 68, box.width - 40, box.height - 88);
        g2.setColor(new Color(30, 110, 75));
        g2.drawOval(box.x + 150, box.y + 95, 280, 280);
        g2.drawLine(box.x + 20, box.y + 235, box.x + box.width - 20, box.y + 235);
        g2.drawLine(box.x + 290, box.y + 68, box.x + 290, box.y + box.height - 20);

        // Asteroids
        for (Asteroid a : asteroids) {
            int ax = (int) (box.x + a.x);
            int ay = (int) (box.y + a.y);
            int r = (int) a.radius;
            g2.setColor(new Color(145, 115, 88));
            g2.fillOval(ax - r, ay - r, r * 2, r * 2);
            g2.setColor(new Color(95, 72, 54));
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(ax - r, ay - r, r * 2, r * 2);
        }

        // Laser fire beams
        if (laserFlash > 0 && lastLaserX >= 0) {
            g2.setColor(new Color(65, 255, 140));
            g2.setStroke(new BasicStroke(3f));
            g2.drawLine(box.x + 24, box.y + box.height - 24, lastLaserX, lastLaserY);
            g2.drawLine(box.x + box.width - 24, box.y + box.height - 24, lastLaserX, lastLaserY);
        }
    }
}