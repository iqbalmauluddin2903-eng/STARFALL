package starfall.ui;

import starfall.entities.Character;
import starfall.entities.Player;
import starfall.entities.PlayerRole;
import starfall.gameplay.VotingManager;
import starfall.graphics.SpriteManager;

import java.awt.*;
import java.util.List;
import java.util.Random;

public class EjectionScreen {

    private static final double TOTAL_DURATION = 4.5;
    private final SpriteManager spriteManager = new SpriteManager();
    private final double[] starX = new double[120];
    private final double[] starY = new double[120];

    private double timer = 0.0;
    private Character ejectedChar = null;
    private String verdictText = "";
    private String remainingText = "";

    public EjectionScreen() {
        Random rng = new Random(99);
        for (int i = 0; i < starX.length; i++) {
            starX[i] = rng.nextDouble() * 1600;
            starY[i] = rng.nextDouble() * 1000;
        }
    }

    public void start(VotingManager votingMgr, List<Player> players) {
        this.timer = 0.0;
        this.ejectedChar = null;

        String ejectedId = votingMgr.getEjectedPlayerId();
        if (ejectedId != null) {
            for (Player p : players) {
                if (p.getPlayerId().equals(ejectedId)) {
                    ejectedChar = p.getCharacter();
                    break;
                }
            }
        }

        if (ejectedChar != null) {
            boolean wasImp = (ejectedChar.getRole() == PlayerRole.IMPOSTOR);
            ejectedChar.becomeGhost();
            verdictText = ejectedChar.getName() + (wasImp ? " was The Impostor." : " was not The Impostor.");
        } else if (votingMgr.isWasTie()) {
            verdictText = "No one was ejected (Tie).";
        } else {
            verdictText = "No one was ejected (Skipped).";
        }

        int remainingImpostors = 0;
        for (Player p : players) {
            Character c = p.getCharacter();
            if (c.isAlive() && c.getRole() == PlayerRole.IMPOSTOR) {
                remainingImpostors++;
            }
        }
        remainingText = remainingImpostors + (remainingImpostors == 1 ? " Impostor remains." : " Impostors remain.");
    }

    public boolean update(double dt) {
        timer += dt;
        return timer >= TOTAL_DURATION;
    }

    public void render(Graphics2D g2, int screenW, int screenH) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(4, 6, 12));
        g2.fillRect(0, 0, screenW, screenH);

        // Starfield
        g2.setColor(new Color(225, 235, 255, 190));
        for (int i = 0; i < starX.length; i++) {
            int sx = (int) ((starX[i] - timer * 40) % screenW);
            if (sx < 0) sx += screenW;
            int sy = (int) (starY[i] % screenH);
            g2.fillOval(sx, sy, 2, 2);
        }

        // Floating spinning ejected character
        if (ejectedChar != null) {
            double progress = Math.min(1.0, timer / TOTAL_DURATION);
            double ex = -60 + progress * (screenW + 120);
            double ey = screenH * 0.42 + Math.sin(timer * 2.5) * 28.0;

            Graphics2D eg = (Graphics2D) g2.create();
            eg.translate(ex, ey);
            eg.rotate(timer * 2.2);
            eg.scale(1.5, 1.5);

            // Draw character body at local origin
            eg.setColor(ejectedChar.getColor().getMainColor());
            eg.fillRoundRect(-14, -24, 28, 36, 24, 24);
            eg.setColor(Color.BLACK);
            eg.setStroke(new BasicStroke(2.5f));
            eg.drawRoundRect(-14, -24, 28, 36, 24, 24);
            eg.setColor(new Color(165, 228, 252));
            eg.fillRoundRect(0, -16, 16, 11, 10, 10);
            eg.setColor(Color.BLACK);
            eg.drawRoundRect(0, -16, 16, 11, 10, 10);
            eg.dispose();
        }

        // Typewriter reveal of verdict
        int visibleChars = Math.min(verdictText.length(), (int) (timer * 22));
        String shown = verdictText.substring(0, Math.max(0, visibleChars));

        g2.setFont(new Font("SansSerif", Font.BOLD, 32));
        int tw = g2.getFontMetrics().stringWidth(verdictText);
        g2.setColor(Color.WHITE);
        g2.drawString(shown, (screenW - tw) / 2, (int) (screenH * 0.56));

        if (timer > 2.0) {
            g2.setFont(new Font("Consolas", Font.BOLD, 18));
            int rw = g2.getFontMetrics().stringWidth(remainingText);
            g2.setColor(new Color(160, 200, 240));
            g2.drawString(remainingText, (screenW - rw) / 2, (int) (screenH * 0.63));
        }
    }
}