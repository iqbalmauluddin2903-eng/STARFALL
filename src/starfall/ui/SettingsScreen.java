package starfall.ui;

import starfall.audio.AudioManager;
import starfall.core.GameConfig;
import starfall.core.GameEngine;
import starfall.core.GameState;
import starfall.core.InputManager;

import java.awt.*;

public class SettingsScreen {

    public void update(double dt, InputManager input, int w, int h, GameEngine engine) {
        if (!input.consumeLeftClick()) return;

        int mx = input.getMouseX();
        int my = input.getMouseY();
        GameConfig cfg = engine.getConfig();
        AudioManager audio = engine.getAudioManager();

        int col1X = 110;
        int col2X = w / 2 + 30;
        int startY = 140;
        int rowH = 58;

        // Left Column: Audio & Display
        if (handleVolumeButtons(mx, my, col1X + 220, startY, cfg.getMasterVolume(), val -> {
            cfg.setMasterVolume(val);
            audio.playButtonClick();
        })) return;

        if (handleVolumeButtons(mx, my, col1X + 220, startY + rowH, cfg.getMusicVolume(), val -> {
            cfg.setMusicVolume(val);
            audio.playButtonClick();
        })) return;

        if (handleVolumeButtons(mx, my, col1X + 220, startY + rowH * 2, cfg.getSfxVolume(), val -> {
            cfg.setSfxVolume(val);
            audio.playButtonClick();
        })) return;

        // Resolution toggle
        if (new Rectangle(col1X + 220, startY + rowH * 3, 210, 38).contains(mx, my)) {
            cfg.cycleResolution();
            engine.applyResolutionChange(cfg.getWindowWidth(), cfg.getWindowHeight());
            audio.playButtonClick();
            return;
        }

        // Fullscreen toggle
        if (new Rectangle(col1X + 220, startY + rowH * 4, 210, 38).contains(mx, my)) {
            engine.toggleFullscreenMode();
            audio.playButtonClick();
            return;
        }

        // FPS / Debug HUD toggle
        if (new Rectangle(col1X + 220, startY + rowH * 5, 210, 38).contains(mx, my)) {
            cfg.toggleDebugHud();
            audio.playButtonClick();
            return;
        }

        // Right Column: Graphics, Gameplay Rules, Controls & Language
        if (new Rectangle(col2X + 230, startY, 210, 38).contains(mx, my)) {
            cfg.setGraphicsQuality(cfg.getGraphicsQuality().next());
            audio.playButtonClick();
            return;
        }

        if (new Rectangle(col2X + 230, startY + rowH, 210, 38).contains(mx, my)) {
            int nextCount = cfg.getOfflinePlayerCount() >= 10 ? 4 : cfg.getOfflinePlayerCount() + 1;
            cfg.setOfflinePlayerCount(nextCount);
            audio.playButtonClick();
            return;
        }

        if (new Rectangle(col2X + 230, startY + rowH * 2, 210, 38).contains(mx, my)) {
            int nextImp = (cfg.getImpostorCount() % 3) + 1;
            cfg.setImpostorCount(nextImp);
            audio.playButtonClick();
            return;
        }

        if (new Rectangle(col2X + 230, startY + rowH * 3, 210, 38).contains(mx, my)) {
            cfg.toggleAnonymousVoting();
            audio.playButtonClick();
            return;
        }

        if (new Rectangle(col2X + 230, startY + rowH * 4, 210, 38).contains(mx, my)) {
            cfg.setControlScheme(cfg.getControlScheme().next());
            input.setAllowArrowKeys(cfg.getControlScheme() == GameConfig.ControlScheme.WASD_AND_ARROWS);
            audio.playButtonClick();
            return;
        }

        if (new Rectangle(col2X + 230, startY + rowH * 5, 210, 38).contains(mx, my)) {
            cfg.setLanguage(cfg.getLanguage().next());
            audio.playButtonClick();
            return;
        }

        // Back Button
        if (new Rectangle(w / 2 - 130, h - 85, 260, 46).contains(mx, my)) {
            audio.playButtonClick();
            engine.transitionToState(GameState.MAIN_MENU);
        }
    }

    private boolean handleVolumeButtons(int mx, int my, int x, int y, float current, java.util.function.Consumer<Float> setter) {
        Rectangle minusBtn = new Rectangle(x, y, 42, 38);
        Rectangle plusBtn = new Rectangle(x + 168, y, 42, 38);
        if (minusBtn.contains(mx, my)) {
            setter.accept(Math.max(0f, current - 0.1f));
            return true;
        }
        if (plusBtn.contains(mx, my)) {
            setter.accept(Math.min(1f, current + 0.1f));
            return true;
        }
        return false;
    }

    public void render(Graphics2D g2, int w, int h, GameEngine engine) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setPaint(new GradientPaint(0, 0, new Color(8, 12, 24), 0, h, new Color(18, 24, 42)));
        g2.fillRect(0, 0, w, h);

        GameConfig cfg = engine.getConfig();

        g2.setFont(new Font("SansSerif", Font.BOLD, 34));
        g2.setColor(Color.WHITE);
        g2.drawString("GAME SETTINGS", 110, 82);

        g2.setFont(new Font("Consolas", Font.PLAIN, 14));
        g2.setColor(new Color(140, 215, 255));
        g2.drawString("Configure audio levels, display resolution, graphics quality, and match rules.", 110, 108);

        int col1X = 110;
        int col2X = w / 2 + 30;
        int startY = 140;
        int rowH = 58;

        // Left Column
        drawVolumeRow(g2, col1X, startY, "MASTER VOLUME", cfg.getMasterVolume());
        drawVolumeRow(g2, col1X, startY + rowH, "MUSIC VOLUME", cfg.getMusicVolume());
        drawVolumeRow(g2, col1X, startY + rowH * 2, "SFX VOLUME", cfg.getSfxVolume());
        drawToggleRow(g2, col1X, startY + rowH * 3, 220, "RESOLUTION", cfg.getResolutionLabel());
        drawToggleRow(g2, col1X, startY + rowH * 4, 220, "DISPLAY MODE", cfg.isFullscreen() ? "FULLSCREEN" : "WINDOWED");
        drawToggleRow(g2, col1X, startY + rowH * 5, 220, "FPS / DEBUG HUD", cfg.isShowDebugHud() ? "SHOWN" : "HIDDEN");

        // Right Column
        drawToggleRow(g2, col2X, startY, 230, "GRAPHICS QUALITY", cfg.getGraphicsQuality().getLabel());
        drawToggleRow(g2, col2X, startY + rowH, 230, "OFFLINE PLAYERS", cfg.getOfflinePlayerCount() + " PLAYERS");
        drawToggleRow(g2, col2X, startY + rowH * 2, 230, "IMPOSTOR COUNT", cfg.getImpostorCount() + " IMPOSTOR(S)");
        drawToggleRow(g2, col2X, startY + rowH * 3, 230, "ANONYMOUS VOTING", cfg.isAnonymousVoting() ? "ENABLED" : "DISABLED");
        drawToggleRow(g2, col2X, startY + rowH * 4, 230, "CONTROLS", cfg.getControlScheme().getLabel());
        drawToggleRow(g2, col2X, startY + rowH * 5, 230, "LANGUAGE", cfg.getLanguage().getLabel());

        // Save & Back Button
        Rectangle backBtn = new Rectangle(w / 2 - 130, h - 85, 260, 46);
        g2.setColor(new Color(46, 213, 115));
        g2.fillRoundRect(backBtn.x, backBtn.y, backBtn.width, backBtn.height, 12, 12);
        g2.setFont(new Font("Consolas", Font.BOLD, 16));
        g2.setColor(Color.BLACK);
        g2.drawString("APPLY & BACK [ESC]", backBtn.x + 36, backBtn.y + 29);
    }

    private void drawVolumeRow(Graphics2D g2, int x, int y, String label, float value) {
        g2.setFont(new Font("Consolas", Font.BOLD, 14));
        g2.setColor(Color.WHITE);
        g2.drawString(label, x, y + 24);

        int ctrlX = x + 220;
        drawMiniButton(g2, ctrlX, y, 42, 38, "-");
        drawMiniButton(g2, ctrlX + 168, y, 42, 38, "+");

        g2.setColor(new Color(28, 36, 54));
        g2.fillRoundRect(ctrlX + 48, y + 6, 114, 26, 6, 6);
        g2.setColor(new Color(0, 215, 255));
        g2.fillRoundRect(ctrlX + 48, y + 6, (int) (114 * value), 26, 6, 6);

        g2.setFont(new Font("Consolas", Font.BOLD, 12));
        g2.setColor(Color.WHITE);
        g2.drawString(Math.round(value * 100) + "%", ctrlX + 88, y + 24);
    }

    private void drawToggleRow(Graphics2D g2, int x, int y, int offset, String label, String valueText) {
        g2.setFont(new Font("Consolas", Font.BOLD, 14));
        g2.setColor(Color.WHITE);
        g2.drawString(label, x, y + 24);

        int bx = x + offset;
        g2.setColor(new Color(32, 44, 68));
        g2.fillRoundRect(bx, y, 210, 38, 10, 10);
        g2.setColor(new Color(0, 210, 255));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(bx, y, 210, 38, 10, 10);

        g2.setFont(new Font("Consolas", Font.BOLD, 13));
        int tw = g2.getFontMetrics().stringWidth(valueText);
        g2.drawString(valueText, bx + (210 - tw) / 2, y + 24);
    }

    private void drawMiniButton(Graphics2D g2, int x, int y, int w, int h, String txt) {
        g2.setColor(new Color(45, 60, 88));
        g2.fillRoundRect(x, y, w, h, 8, 8);
        g2.setColor(new Color(0, 210, 255));
        g2.drawRoundRect(x, y, w, h, 8, 8);
        g2.setFont(new Font("Consolas", Font.BOLD, 16));
        g2.setColor(Color.WHITE);
        g2.drawString(txt, x + 15, y + 25);
    }
}