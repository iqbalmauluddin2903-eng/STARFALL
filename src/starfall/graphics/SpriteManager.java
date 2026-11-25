package starfall.graphics;

import java.awt.*;
import java.awt.geom.*;
import starfall.entities.Character;
import starfall.entities.Cosmetics;
import starfall.entities.MovementState;
import starfall.entities.PlayerRole;
import starfall.map.Vent;
import starfall.map.Wall;

public class SpriteManager {

    public void drawRoomProp(Graphics2D g2, Wall prop) {
        Rectangle r = prop.getBounds();
        Color base = prop.getPrimaryColor();

        switch (prop.getStyle()) {
            case CAFETERIA_TABLE: {
                g2.setColor(new Color(0, 0, 0, 75));
                g2.fillOval(r.x - 4, r.y + 6, r.width + 8, r.height + 8);
                g2.setColor(base);
                g2.fillOval(r.x, r.y, r.width, r.height);
                g2.setColor(new Color(40, 48, 60));
                g2.setStroke(new BasicStroke(3f));
                g2.drawOval(r.x, r.y, r.width, r.height);

                int cx = r.x + r.width / 2;
                int cy = r.y + r.height / 2;
                g2.setColor(new Color(65, 72, 84));
                g2.fillRoundRect(cx - 24, cy - 24, 48, 48, 10, 10);
                g2.setColor(new Color(225, 38, 38));
                g2.fillOval(cx - 16, cy - 16, 32, 32);
                g2.setColor(new Color(255, 140, 140));
                g2.fillOval(cx - 10, cy - 11, 10, 7);
                break;
            }
            case MEDBAY_SCANNER: {
                g2.setColor(new Color(45, 62, 68));
                g2.fillRoundRect(r.x, r.y, r.width, r.height, 18, 18);
                g2.setColor(new Color(40, 235, 175, 130));
                g2.fillOval(r.x + 12, r.y + 12, r.width - 24, r.height - 24);
                g2.setColor(new Color(140, 255, 220));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawOval(r.x + 12, r.y + 12, r.width - 24, r.height - 24);
                break;
            }
            case REACTOR_CORE: {
                g2.setColor(new Color(35, 45, 60));
                g2.fillRoundRect(r.x, r.y, r.width, r.height, 22, 22);
                g2.setColor(new Color(0, 215, 255, 180));
                g2.fillOval(r.x + 15, r.y + 20, r.width - 30, r.height - 40);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(3f));
                g2.drawRoundRect(r.x, r.y, r.width, r.height, 22, 22);
                break;
            }
            default: {
                g2.setColor(new Color(0, 0, 0, 70));
                g2.fillRoundRect(r.x + 4, r.y + 6, r.width, r.height, 12, 12);
                g2.setColor(base);
                g2.fillRoundRect(r.x, r.y, r.width, r.height, 12, 12);
                g2.setColor(base.brighter());
                g2.fillRoundRect(r.x + 6, r.y + 6, r.width - 12, Math.max(8, r.height / 3), 8, 8);
                g2.setColor(new Color(30, 35, 45));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRoundRect(r.x, r.y, r.width, r.height, 12, 12);
                break;
            }
        }
    }

    public void drawVent(Graphics2D g2, Vent vent, boolean highlighted) {
        int vx = (int) vent.getX() - 22;
        int vy = (int) vent.getY() - 14;

        if (highlighted) {
            g2.setColor(new Color(255, 215, 0, 110));
            g2.fillRoundRect(vx - 4, vy - 4, 52, 36, 10, 10);
        }

        g2.setColor(new Color(118, 128, 142));
        g2.fillRoundRect(vx, vy, 44, 28, 7, 7);
        g2.setColor(new Color(32, 38, 46));
        g2.setStroke(new BasicStroke(2.2f));
        g2.drawRoundRect(vx, vy, 44, 28, 7, 7);

        for (int i = 7; i <= 37; i += 7) {
            g2.drawLine(vx + i, vy + 5, vx + i, vy + 23);
        }
    }

    public void drawDeadBodyAtCrimeScene(Graphics2D g2, Character c) {
        if (!c.hasUnreportedBody()) return;
        Graphics2D bg = (Graphics2D) g2.create();
        bg.translate(c.getDeadBodyX(), c.getDeadBodyY());
        drawDeadBody(bg, c);
        bg.dispose();
    }

    public void drawCharacter(Graphics2D g2, Character c, boolean isLocal) {
        if (c.getMovementState() == MovementState.VENTING) return;

        // Draw companion pet for living players
        if (c.getPet() != Cosmetics.Pet.NONE && c.isAlive()) {
            drawPet(g2, c);
        }

        if (!c.isAlive() && c.getRole() != PlayerRole.GHOST) {
            return;
        }

        Graphics2D cg = (Graphics2D) g2.create();
        cg.translate(c.getX(), c.getY());

        boolean isGhost = (c.getRole() == PlayerRole.GHOST);
        if (isGhost) {
            cg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.52f));
        } else {
            cg.setColor(new Color(0, 0, 0, 95));
            cg.fillOval(-18, 13, 36, 11);
        }

        double bob = (c.getMovementState() == MovementState.WALKING)
                ? Math.abs(Math.sin(c.getWalkCycle())) * -3.0
                : Math.sin(c.getIdleTime() * 3.0) * 1.2;
        cg.translate(0, bob);

        if (!c.isFacingRight()) {
            cg.scale(-1, 1);
        }

        Color main = c.getColor().getMainColor();
        Color shadow = c.getColor().getShadowColor();

        // 1. Backpack
        cg.setColor(shadow);
        cg.fillRoundRect(-21, -10, 11, 22, 9, 9);
        cg.setColor(main);
        cg.fillRoundRect(-20, -9, 8, 14, 6, 6);
        cg.setColor(Color.BLACK);
        cg.setStroke(new BasicStroke(2.6f));
        cg.drawRoundRect(-21, -10, 11, 22, 9, 9);

        // 2. Legs or Ghost Tail
        if (isGhost) {
            Path2D tail = new Path2D.Double();
            tail.moveTo(-13, 8);
            double wave = Math.sin(c.getIdleTime() * 6.0) * 4.0;
            tail.curveTo(-6, 22 + wave, 6, 16 - wave, 13, 24);
            tail.lineTo(13, 6);
            tail.closePath();
            cg.setColor(shadow);
            cg.fill(tail);
            cg.setColor(Color.BLACK);
            cg.draw(tail);
        } else {
            int legSwing = (int) (Math.sin(c.getWalkCycle()) * 5.0);
            cg.setColor(shadow);
            cg.fillRoundRect(-12, 8 + legSwing, 10, 12, 7, 7);
            cg.fillRoundRect(2, 8 - legSwing, 10, 12, 7, 7);
            cg.setColor(Color.BLACK);
            cg.drawRoundRect(-12, 8 + legSwing, 10, 12, 7, 7);
            cg.drawRoundRect(2, 8 - legSwing, 10, 12, 7, 7);
        }

        // 3. Main Body
        Shape body = new RoundRectangle2D.Double(-14, -24, 28, 36, 26, 26);
        cg.setColor(shadow);
        cg.fill(body);
        cg.setColor(main);
        cg.fill(new RoundRectangle2D.Double(-11, -23, 24, 28, 22, 22));
        cg.setColor(Color.BLACK);
        cg.setStroke(new BasicStroke(2.8f));
        cg.draw(body);

        // 4. Skin Overlay
        drawSkinOverlay(cg, c.getSkin());

        // 5. Reflective Visor
        Shape visor = new RoundRectangle2D.Double(-1, -16, 18, 12, 11, 11);
        cg.setColor(new Color(95, 160, 192));
        cg.fill(visor);
        cg.setColor(new Color(165, 228, 252));
        cg.fill(new RoundRectangle2D.Double(1, -15, 14, 8, 8, 8));
        cg.setColor(Color.WHITE);
        cg.fillOval(5, -14, 6, 3);
        cg.setColor(Color.BLACK);
        cg.setStroke(new BasicStroke(2.3f));
        cg.draw(visor);

        // 6. Cosmetic Hat
        drawHat(cg, c.getHat());

        cg.dispose();

        // 7. Nameplate
        drawNameplate(g2, c, isLocal);
    }

    private void drawDeadBody(Graphics2D cg, Character c) {
        cg.setColor(new Color(185, 20, 20, 185));
        cg.fillOval(-22, 3, 44, 18);

        cg.setColor(c.getColor().getShadowColor());
        cg.fillRoundRect(-14, 0, 28, 17, 10, 10);
        cg.setColor(Color.BLACK);
        cg.setStroke(new BasicStroke(2.6f));
        cg.drawRoundRect(-14, 0, 28, 17, 10, 10);

        cg.setColor(Color.WHITE);
        cg.fillRect(-3, -13, 6, 15);
        cg.fillOval(-7, -17, 8, 8);
        cg.fillOval(-1, -17, 8, 8);
        cg.setColor(Color.BLACK);
        cg.setStroke(new BasicStroke(1.8f));
        cg.drawOval(-7, -17, 8, 8);
        cg.drawOval(-1, -17, 8, 8);
    }

    private void drawSkinOverlay(Graphics2D cg, Cosmetics.Skin skin) {
        switch (skin) {
            case LAB_COAT -> {
                cg.setColor(new Color(242, 246, 252));
                cg.fillRoundRect(-13, -3, 26, 14, 10, 10);
                cg.setColor(Color.BLACK);
                cg.setStroke(new BasicStroke(2f));
                cg.drawRoundRect(-13, -3, 26, 14, 10, 10);
                cg.drawLine(2, -3, 2, 11);
            }
            case CAPTAIN -> {
                cg.setColor(new Color(26, 38, 68));
                cg.fillRoundRect(-13, -3, 26, 14, 10, 10);
                cg.setColor(new Color(255, 205, 45));
                cg.fillRect(-9, 1, 7, 3);
            }
            case MECHANIC -> {
                cg.setColor(new Color(45, 92, 165));
                cg.fillRoundRect(-13, -2, 26, 13, 10, 10);
                cg.setColor(new Color(240, 195, 50));
                cg.fillOval(-5, 0, 4, 4);
            }
            case SECURITY -> {
                cg.setColor(new Color(38, 42, 48));
                cg.fillRoundRect(-13, -3, 26, 14, 10, 10);
                cg.setColor(new Color(80, 185, 255));
                cg.fillRect(3, 0, 6, 4);
            }
            default -> {}
        }
    }

    private void drawHat(Graphics2D cg, Cosmetics.Hat hat) {
        cg.setStroke(new BasicStroke(2.2f));
        switch (hat) {
            case CROWN -> {
                int[] cx = {-10, -10, -4, 0, 4, 10, 10};
                int[] cy = {-22, -33, -27, -35, -27, -33, -22};
                cg.setColor(new Color(255, 210, 35));
                cg.fillPolygon(cx, cy, 7);
                cg.setColor(Color.BLACK);
                cg.drawPolygon(cx, cy, 7);
            }
            case ASTRONAUT -> {
                cg.setColor(new Color(210, 240, 255, 95));
                cg.fillOval(-18, -28, 36, 30);
                cg.setColor(new Color(235, 248, 255));
                cg.drawOval(-18, -28, 36, 30);
            }
            case COWBOY -> {
                cg.setColor(new Color(142, 88, 42));
                cg.fillOval(-18, -26, 36, 8);
                cg.fillRoundRect(-10, -35, 20, 12, 6, 6);
                cg.setColor(Color.BLACK);
                cg.drawOval(-18, -26, 36, 8);
                cg.drawRoundRect(-10, -35, 20, 12, 6, 6);
            }
            case VIKING -> {
                cg.setColor(new Color(130, 140, 155));
                cg.fillArc(-12, -32, 24, 18, 0, 180);
                cg.setColor(new Color(245, 240, 220));
                cg.fillPolygon(new int[]{-11, -18, -9}, new int[]{-25, -34, -29}, 3);
                cg.fillPolygon(new int[]{11, 18, 9}, new int[]{-25, -34, -29}, 3);
                cg.setColor(Color.BLACK);
                cg.drawArc(-12, -32, 24, 18, 0, 180);
            }
            case BEANIE -> {
                cg.setColor(new Color(215, 45, 55));
                cg.fillArc(-12, -33, 24, 20, 0, 180);
                cg.fillOval(-4, -37, 8, 8);
                cg.setColor(Color.BLACK);
                cg.drawArc(-12, -33, 24, 20, 0, 180);
            }
            case LEAF -> {
                cg.setColor(new Color(55, 195, 65));
                cg.fillRect(-1, -31, 3, 9);
                cg.fillOval(-10, -36, 11, 7);
                cg.fillOval(0, -35, 11, 7);
            }
            default -> {}
        }
    }

    private void drawPet(Graphics2D g2, Character owner) {
        Graphics2D pg = (Graphics2D) g2.create();
        pg.translate(owner.getPetX(), owner.getPetY());
        double hop = Math.abs(Math.sin(owner.getIdleTime() * 5.0)) * -3.0;
        pg.translate(0, hop);

        switch (owner.getPet()) {
            case MINI_CREW -> {
                pg.setColor(owner.getColor().getMainColor());
                pg.fillRoundRect(-7, -11, 14, 18, 12, 12);
                pg.setColor(Color.BLACK);
                pg.setStroke(new BasicStroke(2f));
                pg.drawRoundRect(-7, -11, 14, 18, 12, 12);
                pg.setColor(new Color(165, 228, 252));
                pg.fillRoundRect(-1, -7, 9, 6, 5, 5);
            }
            case UFO_DRONE -> {
                pg.setColor(new Color(140, 155, 175));
                pg.fillOval(-11, -8, 22, 10);
                pg.setColor(new Color(80, 245, 220));
                pg.fillArc(-6, -13, 12, 10, 0, 180);
            }
            case SPACE_DOG -> {
                pg.setColor(new Color(185, 135, 85));
                pg.fillRoundRect(-9, -8, 18, 12, 8, 8);
                pg.fillOval(4, -13, 10, 10);
                pg.setColor(Color.BLACK);
                pg.drawRoundRect(-9, -8, 18, 12, 8, 8);
            }
            default -> {}
        }
        pg.dispose();
    }

    private void drawNameplate(Graphics2D g2, Character c, boolean isLocal) {
        g2.setFont(new Font("Consolas", Font.BOLD, 13));
        String label = c.getName();
        int sw = g2.getFontMetrics().stringWidth(label);
        int tx = (int) c.getX() - sw / 2;
        int ty = (int) c.getY() - 36;

        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(label, tx + 1, ty + 1);
        g2.setColor(isLocal ? new Color(110, 255, 160) : Color.WHITE);
        g2.drawString(label, tx, ty);
    }
}