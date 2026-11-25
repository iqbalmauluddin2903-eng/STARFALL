package starfall.ui;

import starfall.core.GameConfig;
import starfall.core.GameEngine;
import starfall.core.GameState;
import starfall.core.InputManager;
import starfall.entities.Character;
import starfall.entities.Crewmate;
import starfall.graphics.SpriteManager;

import java.awt.*;
import java.util.Random;

public class MainMenu {

    private static final int STAR_COUNT = 200;
    private final double[] starX = new double[STAR_COUNT];
    private final double[] starY = new double[STAR_COUNT];
    private final double[] starSpeed = new double[STAR_COUNT];
    private final int[] starSize = new int[STAR_COUNT];

    private final SpriteManager spriteManager = new SpriteManager();
    private final double[] hoverSlide = new double[8];
    private int lastHoveredIdx = -1;
    private double animTime = 0.0;

    public MainMenu() {
        Random rng = new Random(77);
        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i] = rng.nextDouble() * 1920;
            starY[i] = rng.nextDouble() * 1080;
            int layer = rng.nextInt(3);
            starSpeed[i] = 20.0 + layer * 30.0;
            starSize[i] = 1 + layer;
        }
    }

    public void update(double dt, InputManager input, int w, int h, GameEngine engine) {
        animTime += dt;
        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i] -= starSpeed[i] * dt;
            if (starX[i] < -4) {
                starX[i] = w + 4;
                starY[i] = (starY[i] + 157.3) % Math.max(1, h);
            }
        }

        // Sub-screens: HOW_TO_PLAY & CREDITS
        GameState state = engine.getState();
        if (state == GameState.HOW_TO_PLAY || state == GameState.CREDITS) {
            Rectangle backBtn = new Rectangle(w / 2 - 120, h - 88, 240, 44);
            if (input.consumeLeftClick() && backBtn.contains(input.getMouseX(), input.getMouseY())) {
                engine.getAudioManager().playButtonClick();
                engine.transitionToState(GameState.MAIN_MENU);
            }
            return;
        }

        int mx = input.getMouseX();
        int my = input.getMouseY();
        boolean clicked = input.consumeLeftClick();

        // 8 Main Menu Buttons on Left Column
        int startX = 76;
        int startY = 205;
        int btnW = 310;
        int btnH = 46;
        int gap = 10;

        int hoveredNow = -1;
        for (int i = 0; i < 8; i++) {
            Rectangle r = new Rectangle(startX, startY + i * (btnH + gap), btnW, btnH);
            boolean isOver = r.contains(mx, my);
            if (isOver) {
                hoveredNow = i;
                hoverSlide[i] = Math.min(18.0, hoverSlide[i] + dt * 130.0);
                if (clicked) {
                    engine.getAudioManager().playButtonClick();
                    executeMenuOption(i, engine);
                    return;
                }
            } else {
                hoverSlide[i] = Math.max(0.0, hoverSlide[i] - dt * 110.0);
            }
        }

        if (hoveredNow != -1 && hoveredNow != lastHoveredIdx) {
            engine.getAudioManager().playButtonHover();
        }
        lastHoveredIdx = hoveredNow;

        // Character customization quick buttons on Right Showcase Panel
        int panelX = w - 410;
        int panelY = 225;
        if (clicked) {
            if (new Rectangle(panelX + 35, panelY + 235, 145, 36).contains(mx, my)) {
                engine.getAudioManager().playButtonClick();
                engine.getLocalPlayer().cycleColor();
            } else if (new Rectangle(panelX + 195, panelY + 235, 145, 36).contains(mx, my)) {
                engine.getAudioManager().playButtonClick();
                engine.getLocalPlayer().cycleHat();
            } else if (new Rectangle(panelX + 35, panelY + 282, 145, 36).contains(mx, my)) {
                engine.getAudioManager().playButtonClick();
                engine.getLocalPlayer().cycleSkin();
            } else if (new Rectangle(panelX + 195, panelY + 282, 145, 36).contains(mx, my)) {
                engine.getAudioManager().playButtonClick();
                engine.getLocalPlayer().cyclePet();
            }
        }
    }

    private void executeMenuOption(int index, GameEngine engine) {
        switch (index) {
            case 0 -> { // 1. Play Online
                engine.getConfig().setPrivateRoom(false);
                engine.openMultiplayerLobby(false);
            }
            case 1 -> { // 2. Create Private Room
                engine.getConfig().setPrivateRoom(true);
                engine.openMultiplayerLobby(true);
            }
            case 2 -> { // 3. Join Room
                engine.openMultiplayerLobby(false);
            }
            case 3 -> { // 4. Play Offline
                engine.startOfflineMatchFromConfig();
            }
            case 4 -> { // 5. Settings
                engine.transitionToState(GameState.SETTINGS);
            }
            case 5 -> { // 6. How to Play
                engine.transitionToState(GameState.HOW_TO_PLAY);
            }
            case 6 -> { // 7. Credits
                engine.transitionToState(GameState.CREDITS);
            }
            case 7 -> { // 8. Exit Game
                engine.shutdown();
                System.exit(0);
            }
            default -> {}
        }
    }

    public void render(Graphics2D g2, int w, int h, GameEngine engine) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Deep Space Gradient & Animated Stars
        g2.setPaint(new GradientPaint(0, 0, new Color(7, 10, 20), 0, h, new Color(18, 24, 44)));
        g2.fillRect(0, 0, w, h);

        RadialGradientPaint nebula = new RadialGradientPaint(
                w * 0.72f, h * 0.45f, 420f,
                new float[]{0f, 1f},
                new Color[]{new Color(55, 35, 125, 95), new Color(8, 12, 24, 0)}
        );
        g2.setPaint(nebula);
        g2.fillRect(0, 0, w, h);

        for (int i = 0; i < STAR_COUNT; i++) {
            int alpha = 115 + starSize[i] * 45;
            g2.setColor(new Color(220, 235, 255, Math.min(255, alpha)));
            g2.fillOval((int) starX[i], (int) starY[i], starSize[i], starSize[i]);
        }

        if (engine.getState() == GameState.HOW_TO_PLAY) {
            renderHowToPlay(g2, w, h, engine.getConfig());
            return;
        }
        if (engine.getState() == GameState.CREDITS) {
            renderCredits(g2, w, h);
            return;
        }

        // Title Logo Header
        g2.setFont(new Font("SansSerif", Font.BOLD, 54));
        g2.setColor(new Color(0, 225, 255, 70));
        g2.drawString("STARFALL", 79, 113);
        g2.setColor(Color.WHITE);
        g2.drawString("STARFALL", 76, 110);

        g2.setFont(new Font("Consolas", Font.BOLD, 18));
        g2.setColor(new Color(120, 215, 255));
        g2.drawString("SPACE MYSTERY — SOCIAL DEDUCTION EDITION", 78, 142);

        // 8 Menu Buttons
        boolean isIndo = (engine.getConfig().getLanguage() == GameConfig.Language.ID);
        String[] labels = isIndo ? new String[]{
            "1. MAIN ONLINE",
            "2. BUAT ROOM PRIVAT",
            "3. GABUNG ROOM",
            "4. MAIN OFFLINE (AI)",
            "5. PENGATURAN",
            "6. CARA BERMAIN",
            "7. KREDIT",
            "8. KELUAR GAME"
        } : new String[]{
            "1. PLAY ONLINE",
            "2. CREATE PRIVATE ROOM",
            "3. JOIN ROOM",
            "4. PLAY OFFLINE",
            "5. SETTINGS",
            "6. HOW TO PLAY",
            "7. CREDITS",
            "8. EXIT GAME"
        };

        int startX = 76;
        int startY = 205;
        int btnW = 310;
        int btnH = 46;
        int gap = 10;

        for (int i = 0; i < 8; i++) {
            int bx = startX + (int) hoverSlide[i];
            int by = startY + i * (btnH + gap);
            boolean active = hoverSlide[i] > 1.0;

            Color fill = (i == 7)
                    ? (active ? new Color(210, 55, 55) : new Color(55, 24, 30, 210))
                    : (active ? new Color(0, 195, 240) : new Color(20, 28, 46, 215));
            Color border = (i == 7) ? new Color(255, 95, 95) : new Color(0, 215, 255);

            g2.setColor(fill);
            g2.fillRoundRect(bx, by, btnW, btnH, 12, 12);
            g2.setColor(border);
            g2.setStroke(new BasicStroke(active ? 2.5f : 1.5f));
            g2.drawRoundRect(bx, by, btnW, btnH, 12, 12);

            g2.setFont(new Font("Consolas", Font.BOLD, 16));
            g2.setColor(active && i != 7 ? Color.BLACK : Color.WHITE);
            g2.drawString(labels[i], bx + 20, by + 29);
        }

        // Right Side: Animated Character Preview & Quick Customization Card
        drawCharacterShowcase(g2, w - 410, 225, engine);
    }

    private void drawCharacterShowcase(Graphics2D g2, int x, int y, GameEngine engine) {
        int w = 375;
        int h = 345;

        g2.setColor(new Color(16, 22, 36, 220));
        g2.fillRoundRect(x, y, w, h, 16, 16);
        g2.setColor(new Color(0, 210, 255, 170));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(x, y, w, h, 16, 16);

        Character localChar = engine.getLocalPlayer().getCharacter();
        g2.setFont(new Font("Consolas", Font.BOLD, 15));
        g2.setColor(new Color(0, 225, 255));
        g2.drawString("CREW PROFILE: " + localChar.getName(), x + 24, y + 32);

        // Animated Floating Crewmate Preview
        Graphics2D pg = (Graphics2D) g2.create();
        pg.translate(x + w / 2.0, y + 138 + Math.sin(animTime * 2.8) * 5.0);
        pg.scale(2.35, 2.35);
        Character clone = new Crewmate("menu_prev", "", 0, 0, localChar.getColor());
        clone.setHat(localChar.getHat());
        clone.setSkin(localChar.getSkin());
        spriteManager.drawCharacter(pg, clone, false);
        pg.dispose();

        // Cosmetic Toggle Buttons
        drawMiniBtn(g2, x + 35, y + 235, 145, 36, "COLOR: " + localChar.getColor().getDisplayName(), localChar.getColor().getMainColor());
        drawMiniBtn(g2, x + 195, y + 235, 145, 36, "HAT: " + localChar.getHat().name(), new Color(48, 62, 88));
        drawMiniBtn(g2, x + 35, y + 282, 145, 36, "SKIN: " + localChar.getSkin().name(), new Color(48, 62, 88));
        drawMiniBtn(g2, x + 195, y + 282, 145, 36, "PET: " + localChar.getPet().name(), new Color(48, 62, 88));
    }

    private void drawMiniBtn(Graphics2D g2, int x, int y, int w, int h, String text, Color bg) {
        g2.setColor(bg);
        g2.fillRoundRect(x, y, w, h, 8, 8);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, w, h, 8, 8);

        g2.setFont(new Font("Consolas", Font.BOLD, 11));
        int tw = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, x + (w - tw) / 2, y + 22);
    }

    private void renderHowToPlay(Graphics2D g2, int w, int h, GameConfig config) {
        Rectangle box = new Rectangle(90, 55, w - 180, h - 165);
        g2.setColor(new Color(16, 22, 36, 235));
        g2.fillRoundRect(box.x, box.y, box.width, box.height, 18, 18);
        g2.setColor(new Color(0, 215, 255));
        g2.drawRoundRect(box.x, box.y, box.width, box.height, 18, 18);

        g2.setFont(new Font("SansSerif", Font.BOLD, 28));
        g2.setColor(Color.WHITE);
        g2.drawString("HOW TO PLAY — STARFALL: SPACE MYSTERY", box.x + 30, box.y + 46);

        String[] lines = {
            "• CREWMATES: Complete all assigned mini-game tasks [E] across 14 ship rooms or vote out all Impostors.",
            "• IMPOSTORS: Eliminate Crewmates [Q] when no witnesses are around, hide in Vents [V], and Sabotage [1-5].",
            "• EMERGENCY MEETINGS: Press [E] at the Cafeteria table button or press [R] to report a discovered body.",
            "• SABOTAGES: [1] Reactor Meltdown, [2] O2 Depletion, [3] Lights Out, [4] Comms Jam, [5] Door Lockdown.",
            "• GHOSTS: Eliminated players become Ghosts who can pass through walls and finish remaining tasks.",
            "• CONTROLS: Move with WASD / Arrows | [E] Interact | [R] Report | [Q] Kill | [V] Vent | [ESC] Menu."
        };

        g2.setFont(new Font("Consolas", Font.PLAIN, 15));
        int ly = box.y + 105;
        for (String line : lines) {
            g2.setColor(new Color(210, 230, 250));
            g2.drawString(line, box.x + 34, ly);
            ly += 52;
        }

        drawMiniBtn(g2, w / 2 - 120, h - 88, 240, 44, "BACK TO MENU", new Color(0, 195, 240));
    }

    private void renderCredits(Graphics2D g2, int w, int h) {
        Rectangle box = new Rectangle(160, 75, w - 320, h - 195);
        g2.setColor(new Color(16, 22, 36, 235));
        g2.fillRoundRect(box.x, box.y, box.width, box.height, 18, 18);
        g2.setColor(new Color(0, 215, 255));
        g2.drawRoundRect(box.x, box.y, box.width, box.height, 18, 18);

        g2.setFont(new Font("SansSerif", Font.BOLD, 30));
        g2.setColor(Color.WHITE);
        g2.drawString("CREDITS & ARCHITECTURE", box.x + 36, box.y + 52);

        String[] credits = {
            "Title: STARFALL: SPACE MYSTERY (Pure Java SE 17+)",
            "Graphics & UI: Java Swing & Java 2D Graphics2D API",
            "Audio Engine: Java Sound API (javax.sound.sampled Procedural PCM)",
            "Multiplayer: Authoritative TCP Socket Server & Client Architecture",
            "AI System: State-Machine Crewmate & Impostor Agents with BFS Pathfinding",
            "Developed for Object-Oriented Programming & Concurrency Engineering."
        };

        g2.setFont(new Font("Consolas", Font.BOLD, 15));
        int cy = box.y + 115;
        for (String line : credits) {
            g2.setColor(new Color(165, 225, 255));
            g2.drawString(line, box.x + 36, cy);
            cy += 46;
        }

        drawMiniBtn(g2, w / 2 - 120, h - 88, 240, 44, "BACK TO MENU", new Color(0, 195, 240));
    }
}