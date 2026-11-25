package starfall.graphics;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class ParticleSystem {

    private static class Particle {
        double x, y, vx, vy;
        double life, maxLife;
        int size;
        Color color;

        Particle(double x, double y, double vx, double vy, double maxLife, int size, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.life = maxLife;
            this.maxLife = maxLife;
            this.size = size;
            this.color = color;
        }
    }

    private final List<Particle> particles = new ArrayList<>();
    private final Random rng = new Random();

    public void clear() {
        particles.clear();
    }

    public void spawnKillBurst(double x, double y, Color victimColor) {
        for (int i = 0; i < 26; i++) {
            double angle = rng.nextDouble() * Math.PI * 2.0;
            double speed = 50.0 + rng.nextDouble() * 170.0;
            Color col = (i % 3 == 0) ? victimColor : new Color(225, 32, 32);
            particles.add(new Particle(x, y, Math.cos(angle) * speed, Math.sin(angle) * speed,
                    0.45 + rng.nextDouble() * 0.35, 5 + rng.nextInt(5), col));
        }
    }

    public void spawnVentPuff(double x, double y) {
        for (int i = 0; i < 16; i++) {
            double angle = rng.nextDouble() * Math.PI * 2.0;
            double speed = 35.0 + rng.nextDouble() * 95.0;
            particles.add(new Particle(x, y, Math.cos(angle) * speed, Math.sin(angle) * speed,
                    0.35 + rng.nextDouble() * 0.25, 6 + rng.nextInt(5), new Color(175, 195, 220)));
        }
    }

    public void spawnTaskSparkles(double x, double y) {
        for (int i = 0; i < 20; i++) {
            double angle = rng.nextDouble() * Math.PI * 2.0;
            double speed = 40.0 + rng.nextDouble() * 125.0;
            Color col = (i % 2 == 0) ? new Color(75, 245, 145) : new Color(255, 230, 80);
            particles.add(new Particle(x, y, Math.cos(angle) * speed, Math.sin(angle) * speed,
                    0.50 + rng.nextDouble() * 0.30, 4 + rng.nextInt(4), col));
        }
    }

    public void spawnSabotageSparks(double x, double y) {
        for (int i = 0; i < 4; i++) {
            double angle = rng.nextDouble() * Math.PI * 2.0;
            double speed = 45.0 + rng.nextDouble() * 95.0;
            particles.add(new Particle(x, y, Math.cos(angle) * speed, Math.sin(angle) * speed,
                    0.28 + rng.nextDouble() * 0.20, 4, new Color(255, 95, 45)));
        }
    }

    public void update(double dt) {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.vx *= 0.94;
            p.vy *= 0.94;
            p.life -= dt;
            if (p.life <= 0.0) {
                it.remove();
            }
        }
    }

    public void render(Graphics2D g2) {
        for (Particle p : particles) {
            float ratio = (float) Math.max(0.0, Math.min(1.0, p.life / p.maxLife));
            int alpha = (int) (ratio * 235);
            g2.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), alpha));
            int s = Math.max(2, (int) (p.size * ratio));
            g2.fillOval((int) p.x - s / 2, (int) p.y - s / 2, s, s);
        }
    }
}