package starfall.ui;

import starfall.entities.Character;
import starfall.entities.Player;
import starfall.entities.PlayerRole;
import starfall.gameplay.WinConditionManager;

import java.awt.*;
import java.util.List;

public class GameOverScreen {

    public void render(Graphics2D g2, int screenW, int screenH,
                       WinConditionManager.MatchOutcome outcome, List<Player> players) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(6, 9, 16, 232));
        g2.fillRect(0, 0, screenW, screenH);

        if (outcome == null) return;

        Color accent = switch (outcome.getWinner()) {
            case CREWMATES -> new Color(0, 225, 255);
            case IMPOSTORS -> new Color(255, 65, 65);
            case DRAW -> new Color(255, 205, 60);
        };

        // Radial glow behind title
        RadialGradientPaint glow = new RadialGradientPaint(
                screenW / 2f, screenH * 0.25f, 380f,
                new float[]{0f, 1f},
                new Color[]{new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 95), new Color(0, 0, 0, 0)}
        );
        g2.setPaint(glow);
        g2.fillRect(0, 0, screenW, screenH);

        // Victory / Defeat Title
        g2.setFont(new Font("SansSerif", Font.BOLD, 52));
        int tw = g2.getFontMetrics().stringWidth(outcome.getHeadline());
        g2.setColor(accent);
        g2.drawString(outcome.getHeadline(), (screenW - tw) / 2, (int) (screenH * 0.22));

        // Reason Subtitle
        g2.setFont(new Font("Consolas", Font.BOLD, 17));
        int rw = g2.getFontMetrics().stringWidth(outcome.getReason());
        g2.setColor(Color.WHITE);
        g2.drawString(outcome.getReason(), (screenW - rw) / 2, (int) (screenH * 0.29));

        // Player Roster & Role Reveal Cards
        int cardW = 165;
        int cardH = 175;
        int spacing = 22;
        int totalW = players.size() * cardW + Math.max(0, players.size() - 1) * spacing;
        int startX = (screenW - totalW) / 2;
        int startY = (int) (screenH * 0.37);

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            Character c = p.getCharacter();
            int cx = startX + i * (cardW + spacing);

            boolean wasImp = (c.getRole() == PlayerRole.IMPOSTOR)
                    || (c instanceof starfall.entities.Ghost g && g.getOriginalRole() == PlayerRole.IMPOSTOR);

            g2.setColor(new Color(20, 26, 40));
            g2.fillRoundRect(cx, startY, cardW, cardH, 14, 14);
            g2.setColor(wasImp ? new Color(255, 75, 75) : new Color(0, 210, 255));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(cx, startY, cardW, cardH, 14, 14);

            // Character silhouette
            int ax = cx + cardW / 2;
            int ay = startY + 66;
            g2.setColor(c.getColor().getMainColor());
            g2.fillRoundRect(ax - 18, ay - 28, 36, 46, 26, 26);
            g2.setColor(Color.BLACK);
            g2.drawRoundRect(ax - 18, ay - 28, 36, 46, 26, 26);
            g2.setColor(new Color(165, 228, 252));
            g2.fillRoundRect(ax - 2, ay - 18, 22, 13, 10, 10);

            // Name & True Role
            g2.setFont(new Font("Consolas", Font.BOLD, 14));
            int nw = g2.getFontMetrics().stringWidth(c.getName());
            g2.setColor(Color.WHITE);
            g2.drawString(c.getName(), cx + (cardW - nw) / 2, startY + 114);

            String roleLabel = wasImp ? "IMPOSTOR" : "CREWMATE";
            int roleW = g2.getFontMetrics().stringWidth(roleLabel);
            g2.setColor(wasImp ? new Color(255, 85, 85) : new Color(80, 240, 150));
            g2.drawString(roleLabel, cx + (cardW - roleW) / 2, startY + 136);

            g2.setFont(new Font("Consolas", Font.PLAIN, 12));
            String status = c.isAlive() ? "ALIVE" : "ELIMINATED";
            int sw = g2.getFontMetrics().stringWidth(status);
            g2.setColor(c.isAlive() ? Color.LIGHT_GRAY : Color.GRAY);
            g2.drawString(status, cx + (cardW - sw) / 2, startY + 158);
        }

        // Play Again Prompt
        g2.setFont(new Font("Consolas", Font.BOLD, 16));
        String prompt = "Press [SPACE] or [F2] for New Match  |  [ESC] Exit";
        int pw = g2.getFontMetrics().stringWidth(prompt);
        g2.setColor(new Color(255, 220, 90));
        g2.drawString(prompt, (screenW - pw) / 2, (int) (screenH * 0.78));
    }
}