package starfall.graphics;

import starfall.core.GameConfig;
import starfall.core.GameEngine;
import starfall.core.GameState;
import starfall.core.InputManager;
import starfall.entities.*;
import starfall.entities.Character;
import starfall.gameplay.SabotageManager;
import starfall.gameplay.SabotageType;
import starfall.gameplay.TaskManager;
import starfall.map.*;
import starfall.network.NetworkManager;
import starfall.tasks.Task;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class Renderer {

    private static final int STAR_COUNT = 160;
    private final double[] starX = new double[STAR_COUNT];
    private final double[] starY = new double[STAR_COUNT];
    private final double[] starSpeed = new double[STAR_COUNT];
    private final int[] starSize = new int[STAR_COUNT];

    private final SpriteManager spriteManager = new SpriteManager();
    private double alertPulse = 0.0;

    public Renderer(int width, int height) {
        Random rng = new Random(42);
        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i] = rng.nextDouble() * width;
            starY[i] = rng.nextDouble() * height;
            int layer = rng.nextInt(3);
            starSpeed[i] = 14.0 + layer * 22.0;
            starSize[i] = 1 + layer;
        }
    }

    public void update(double dt, int width, int height, InputManager input) {
        alertPulse += dt * 5.0;
        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i] -= starSpeed[i] * dt;
            if (starX[i] < -4) {
                starX[i] = width + 4;
                starY[i] = (starY[i] + 137.5) % Math.max(1, height);
            }
        }
    }

    public void render(Graphics2D g2, int width, int height, GameEngine engine) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        GameState state = engine.getState();

        if (state == GameState.MAIN_MENU || state == GameState.HOW_TO_PLAY || state == GameState.CREDITS) {
            engine.getMainMenu().render(g2, width, height, engine);
            drawScreenTransition(g2, width, height, engine.getScreenFadeAlpha());
            return;
        }
        if (state == GameState.SETTINGS) {
            engine.getSettingsScreen().render(g2, width, height, engine);
            drawScreenTransition(g2, width, height, engine.getScreenFadeAlpha());
            return;
        }
        if (state == GameState.LOBBY) {
            engine.getLobbyScreen().render(g2, width, height, engine);
            drawScreenTransition(g2, width, height, engine.getScreenFadeAlpha());
            return;
        }
        if (state == GameState.EJECTION) {
            engine.getEjectionScreen().render(g2, width, height);
            drawScreenTransition(g2, width, height, engine.getScreenFadeAlpha());
            return;
        }
        if (state == GameState.GAME_OVER) {
            engine.getGameOverScreen().render(g2, width, height, engine.getMatchOutcome(), engine.getPlayers());
            drawScreenTransition(g2, width, height, engine.getScreenFadeAlpha());
            return;
        }

        // 1. Deep Space Starfield
        g2.setColor(new Color(8, 11, 20));
        g2.fillRect(0, 0, width, height);

        int starLimit = (engine.getConfig().getGraphicsQuality() == GameConfig.GraphicsQuality.LOW) ? 70 : STAR_COUNT;
        for (int i = 0; i < starLimit; i++) {
            int alpha = 110 + (starSize[i] * 42);
            g2.setColor(new Color(220, 235, 255, Math.min(255, alpha)));
            g2.fillOval((int) starX[i], (int) starY[i], starSize[i], starSize[i]);
        }

        GameMap map = engine.getGameMap();
        Camera camera = engine.getCamera();
        Character localChar = engine.getLocalPlayer().getCharacter();
        SabotageManager sabotageMgr = engine.getSabotageManager();
        TaskManager taskMgr = engine.getTaskManager();

        // 2. World Rendering with Camera
        Graphics2D wg = (Graphics2D) g2.create();
        wg.translate(-camera.getX(), -camera.getY());

        renderWorld(wg, map, localChar, sabotageMgr);

        // Draw any unreported dead bodies on the floor first
        for (Player p : engine.getPlayers()) {
            spriteManager.drawDeadBodyAtCrimeScene(wg, p.getCharacter());
        }

        List<Character> chars = new ArrayList<>();
        for (Player p : engine.getPlayers()) {
            chars.add(p.getCharacter());
        }
        chars.sort(Comparator.comparingDouble(Character::getY));

        for (Character c : chars) {
            if (c.getRole() == PlayerRole.GHOST && localChar.getRole() != PlayerRole.GHOST && c != localChar) {
                continue;
            }
            spriteManager.drawCharacter(wg, c, c == localChar);
        }

        if (engine.getConfig().getGraphicsQuality() != GameConfig.GraphicsQuality.LOW) {
            engine.getParticleSystem().render(wg);
        }

        wg.dispose();

        // 3. Fog of War / Dynamic Lighting
        drawLightingOverlay(g2, width, height, camera, localChar, sabotageMgr);

        // 4. Sabotage Warning Banner
        drawSabotageWarning(g2, width, height, sabotageMgr);

        // 5. Task Progress Bar & Assigned Task List
        drawTaskHud(g2, taskMgr, localChar, sabotageMgr, engine.getNetworkManager());

        // 6. Minimap
        boolean hideMinimap = sabotageMgr.isCommsSabotaged() && localChar.getRole() == PlayerRole.CREWMATE;
        if (!hideMinimap) {
            drawMinimap(g2, width, map, localChar);
        }

        // 7. Room Transition Banner & Action Bar
        drawRoomBanner(g2, width, localChar.getCurrentRoom(), engine.getRoomBannerAlpha());
        drawActionBar(g2, width, height, engine);

        if (engine.getConfig().isShowDebugHud()) {
            drawDebugHud(g2, engine);
        }

        // 8. Active Modal Overlays (Meeting, Surveillance, or Mini-Game)
        if (state == GameState.MEETING) {
            engine.getMeetingScreen().render(
                    g2, width, height, engine.getPlayers(), engine.getLocalPlayer(),
                    engine.getConfig(), engine.getMeetingManager(), engine.getVotingManager()
            );
        } else if (engine.getSurveillanceOverlay().isOpen()) {
            engine.getSurveillanceOverlay().render(g2, width, height, map, engine.getPlayers());
        } else {
            taskMgr.renderActiveTask(g2, width, height);
        }

        drawScreenTransition(g2, width, height, engine.getScreenFadeAlpha());
    }

    private void drawScreenTransition(Graphics2D g2, int width, int height, float fadeAlpha) {
        if (fadeAlpha <= 0.01f) return;
        g2.setColor(new Color(4, 6, 12, Math.min(255, (int) (fadeAlpha * 255))));
        g2.fillRect(0, 0, width, height);
    }

    private void renderWorld(Graphics2D g2, GameMap map, Character localChar, SabotageManager sabotageMgr) {
        g2.setColor(new Color(44, 52, 66));
        for (Room room : map.getRooms()) {
            Rectangle b = room.getBounds();
            g2.fillRoundRect(b.x - 16, b.y - 16, b.width + 32, b.height + 32, 24, 24);
        }

        for (Room room : map.getRooms()) {
            Rectangle b = room.getBounds();
            g2.setColor(room.getFloorColor());
            g2.fillRect(b.x, b.y, b.width, b.height);

            g2.setColor(new Color(0, 0, 0, 26));
            g2.setStroke(new BasicStroke(1f));
            for (int x = b.x; x < b.x + b.width; x += 44) {
                g2.drawLine(x, b.y, x, b.y + b.height);
            }
            for (int y = b.y; y < b.y + b.height; y += 44) {
                g2.drawLine(b.x, y, b.x + b.width, y);
            }

            if (!room.isCorridor()) {
                g2.setColor(new Color(255, 255, 255, 52));
                g2.setFont(new Font("Consolas", Font.BOLD, 24));
                g2.drawString(room.getDisplayName(), b.x + 18, b.y + 34);
            }
        }

        Vent nearestVent = map.findNearestVent(localChar.getX(), localChar.getY(), localChar.getInteractionRange());
        boolean canSeeVentHighlight = (localChar.getRole() == PlayerRole.IMPOSTOR);
        for (Vent vent : map.getVents()) {
            spriteManager.drawVent(g2, vent, canSeeVentHighlight && vent == nearestVent);
        }

        for (Room room : map.getRooms()) {
            for (Wall prop : room.getProps()) {
                spriteManager.drawRoomProp(g2, prop);
            }
        }

        InteractiveObject nearestObj = map.findNearestInteractable(
                localChar.getX(), localChar.getY(), localChar.getInteractionRange());

        for (InteractiveObject obj : map.getAllInteractables()) {
            boolean isRepairTarget = sabotageMgr.canRepairWithInteractable(obj.getId());
            if (obj.getType() == InteractiveObject.InteractionType.SABOTAGE_FIX && !isRepairTarget) {
                continue;
            }
            if (obj.getType() == InteractiveObject.InteractionType.TASK_TERMINAL && obj.isCompleted() && !isRepairTarget) {
                continue;
            }

            boolean active = (obj == nearestObj);
            int ox = (int) obj.getX();
            int oy = (int) obj.getY();

            Color glow = isRepairTarget ? new Color(255, 65, 65, 130)
                    : (active ? new Color(255, 230, 50, 120) : new Color(255, 215, 0, 50));
            Color ring = isRepairTarget ? new Color(255, 80, 80)
                    : (active ? Color.WHITE : new Color(255, 220, 60));

            g2.setColor(glow);
            g2.fillOval(ox - 22, oy - 22, 44, 44);
            g2.setColor(ring);
            g2.setStroke(new BasicStroke(active ? 3f : 2f));
            g2.drawOval(ox - 16, oy - 16, 32, 32);
        }

        for (Door door : map.getDoors()) {
            Rectangle db = door.getBounds();
            if (door.isClosed()) {
                g2.setColor(new Color(185, 50, 50));
                g2.fillRect(db.x, db.y, db.width, db.height);
                g2.setColor(Color.WHITE);
                g2.drawRect(db.x, db.y, db.width, db.height);
            } else {
                g2.setColor(new Color(60, 72, 90, 120));
                g2.drawRect(db.x, db.y, db.width, db.height);
            }
        }
    }

    private void drawLightingOverlay(Graphics2D g2, int width, int height, Camera camera,
                                     Character localChar, SabotageManager sabotageMgr) {
        if (localChar.getRole() == PlayerRole.GHOST) return;

        double visionRadius;
        int darknessAlpha;
        if (localChar.getRole() == PlayerRole.IMPOSTOR) {
            visionRadius = 560.0;
            darknessAlpha = 145;
        } else if (sabotageMgr.isLightsSabotaged()) {
            visionRadius = 185.0;
            darknessAlpha = 238;
        } else {
            visionRadius = 430.0;
            darknessAlpha = 185;
        }

        double screenX = localChar.getX() - camera.getX();
        double screenY = localChar.getY() - camera.getY();

        Area shadowMask = new Area(new Rectangle(0, 0, width, height));
        Ellipse2D visionCircle = new Ellipse2D.Double(
                screenX - visionRadius, screenY - visionRadius,
                visionRadius * 2, visionRadius * 2
        );
        shadowMask.subtract(new Area(visionCircle));

        g2.setColor(new Color(5, 7, 13, darknessAlpha));
        g2.fill(shadowMask);
    }

    private void drawSabotageWarning(Graphics2D g2, int width, int height, SabotageManager sabotageMgr) {
        SabotageType active = sabotageMgr.getActiveSabotage();
        if (active == SabotageType.NONE) return;

        int flashAlpha = (int) (25 + Math.abs(Math.sin(alertPulse)) * 45);
        g2.setColor(new Color(235, 30, 30, flashAlpha));
        g2.fillRect(0, 0, width, height);

        String timerStr = active.getDurationSeconds() > 0
                ? String.format(" (%.1fs)", sabotageMgr.getCountdown())
                : "";
        String banner = "ALERT: " + active.getWarningTitle() + timerStr + " — REPAIR AT " + active.getRepairRoom().getDisplayName();

        g2.setFont(new Font("Consolas", Font.BOLD, 16));
        int tw = g2.getFontMetrics().stringWidth(banner);
        int bx = (width - tw - 36) / 2;
        int by = 82;

        g2.setColor(new Color(145, 20, 20, 225));
        g2.fillRoundRect(bx, by, tw + 36, 36, 10, 10);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(bx, by, tw + 36, 36, 10, 10);
        g2.drawString(banner, bx + 18, by + 23);
    }

    private void drawTaskHud(Graphics2D g2, TaskManager taskMgr, Character localChar,
                             SabotageManager sabotageMgr, NetworkManager netMgr) {
        int x = 16;
        int y = 16;
        int w = 325;

        g2.setColor(new Color(12, 16, 26, 215));
        g2.fillRoundRect(x, y, w, 46, 10, 10);
        g2.setColor(new Color(90, 115, 155));
        g2.drawRoundRect(x, y, w, 46, 10, 10);

        float pct = taskMgr.getGlobalTaskProgress();
        g2.setColor(new Color(32, 40, 56));
        g2.fillRoundRect(x + 12, y + 22, w - 24, 16, 6, 6);
        g2.setColor(new Color(46, 213, 115));
        g2.fillRoundRect(x + 12, y + 22, (int) ((w - 24) * pct), 16, 6, 6);

        g2.setFont(new Font("Consolas", Font.BOLD, 12));
        g2.setColor(Color.WHITE);
        g2.drawString(String.format("TOTAL SHIP TASKS: %d%%", (int) (pct * 100)), x + 14, y + 16);

        int listY = y + 54;
        boolean commsDown = sabotageMgr.isCommsSabotaged() && localChar.getOriginalRole() == PlayerRole.CREWMATE;
        int listH = commsDown ? 64 : (46 + taskMgr.getAssignedTasks().size() * 20);

        g2.setColor(new Color(12, 16, 26, 200));
        g2.fillRoundRect(x, listY, w, listH, 10, 10);

        boolean isImp = (localChar.getOriginalRole() == PlayerRole.IMPOSTOR);
        g2.setColor(isImp ? new Color(255, 75, 75) : new Color(0, 225, 255));
        String modeTag = netMgr.isConnected() ? " [ONLINE: " + netMgr.getRoomCode() + "]" : " [ESC: Menu]";
        g2.drawString("ROLE: " + localChar.getRole().getDisplayName().toUpperCase() + modeTag, x + 12, listY + 20);

        if (commsDown) {
            g2.setColor(new Color(255, 110, 110));
            g2.drawString("COMMS OFFLINE — Repair at Communications", x + 12, listY + 44);
            return;
        }

        g2.setColor(new Color(200, 215, 235));
        g2.drawString(isImp ? "FAKE TASKS (Sabotage with [1-5]):" : "ASSIGNED TASKS:", x + 12, listY + 38);

        int ty = listY + 56;
        for (Task t : taskMgr.getAssignedTasks()) {
            g2.setColor(t.isCompleted() ? new Color(80, 240, 130) : new Color(245, 225, 120));
            String prefix = t.isCompleted() ? "[✓] " : "[ ] ";
            g2.drawString(prefix + t.getLocation().getDisplayName() + ": " + t.getName(), x + 14, ty);
            ty += 20;
        }
    }

    private void drawMinimap(Graphics2D g2, int screenW, GameMap map, Character localChar) {
        int mw = 240;
        int mh = 150;
        int mx = screenW - mw - 16;
        int my = 16;
        double sx = mw / (double) GameMap.WORLD_WIDTH;
        double sy = mh / (double) GameMap.WORLD_HEIGHT;

        g2.setColor(new Color(10, 14, 24, 215));
        g2.fillRoundRect(mx - 6, my - 6, mw + 12, mh + 26, 12, 12);
        g2.setColor(new Color(0, 210, 255, 160));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(mx - 6, my - 6, mw + 12, mh + 26, 12, 12);

        for (Room room : map.getRooms()) {
            Rectangle r = room.getBounds();
            g2.setColor(room.isCorridor() ? new Color(62, 70, 84) : new Color(98, 114, 142));
            g2.fillRect(
                    mx + (int) (r.x * sx),
                    my + (int) (r.y * sy),
                    Math.max(2, (int) (r.width * sx)),
                    Math.max(2, (int) (r.height * sy))
            );
        }

        int px = mx + (int) (localChar.getX() * sx);
        int py = my + (int) (localChar.getY() * sy);
        g2.setColor(localChar.getColor().getMainColor());
        g2.fillOval(px - 4, py - 4, 8, 8);
        g2.setColor(Color.WHITE);
        g2.drawOval(px - 4, py - 4, 8, 8);

        g2.setFont(new Font("Consolas", Font.BOLD, 12));
        g2.setColor(new Color(255, 220, 90));
        g2.drawString("LOC: " + localChar.getCurrentRoom().getDisplayName(), mx + 4, my + mh + 13);
    }

    private void drawRoomBanner(Graphics2D g2, int screenW, RoomId currentRoom, float alpha) {
        if (alpha <= 0.02f || currentRoom == RoomId.CORRIDOR) return;
        Graphics2D bg = (Graphics2D) g2.create();
        bg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, alpha)));

        String text = currentRoom.getDisplayName();
        bg.setFont(new Font("SansSerif", Font.BOLD, 26));
        int tw = bg.getFontMetrics().stringWidth(text);
        int bx = (screenW - tw - 44) / 2;
        int by = 20;

        bg.setColor(new Color(12, 16, 28, 200));
        bg.fillRoundRect(bx, by, tw + 44, 42, 12, 12);
        bg.setColor(new Color(0, 225, 255));
        bg.setStroke(new BasicStroke(2f));
        bg.drawRoundRect(bx, by, tw + 44, 42, 12, 12);

        bg.setColor(Color.WHITE);
        bg.drawString(text, bx + 22, by + 29);
        bg.dispose();
    }

    private void drawActionBar(Graphics2D g2, int screenW, int screenH, GameEngine engine) {
        Character localChar = engine.getLocalPlayer().getCharacter();
        InteractiveObject nearbyObj = engine.getNearbyInteractable();
        SabotageManager sabMgr = engine.getSabotageManager();

        int barW = 1020;
        int barH = 58;
        int bx = (screenW - barW) / 2;
        int by = screenH - barH - 14;

        g2.setColor(new Color(12, 16, 26, 220));
        g2.fillRoundRect(bx, by, barW, barH, 12, 12);
        g2.setColor(new Color(90, 115, 155, 180));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(bx, by, barW, barH, 12, 12);

        String reportHint = (engine.getReportableBody() != null)
                ? "  |  [R: REPORT " + engine.getReportableBody().getName() + "]"
                : "  |  [R: Report]";

        g2.setFont(new Font("Consolas", Font.BOLD, 12));
        if (localChar instanceof Impostor imp && imp.isAlive()) {
            String killStatus = imp.canKill()
                    ? (engine.getKillTarget() != null ? "READY [Q: KILL " + engine.getKillTarget().getName() + "]" : "NO TARGET [Q]")
                    : String.format("CD (%.1fs)", imp.getKillCooldown());

            String ventStatus = imp.isInsideVent()
                    ? "IN VENT [A/D: Travel | V: Exit]"
                    : (engine.isNearVent() ? "VENT READY [V]" : "VENT [V]");

            String sabStatus = sabMgr.getSabotageCooldown() > 0
                    ? String.format("SAB CD (%.0fs)", sabMgr.getSabotageCooldown())
                    : "SABOTAGE: [1]Reactor [2]O2 [3]Lights [4]Comms [5]Doors  |  [ESC] Main Menu";

            g2.setColor(new Color(255, 95, 95));
            g2.drawString("KILL: " + killStatus + "  |  " + ventStatus + reportHint, bx + 16, by + 22);
            g2.setColor(new Color(255, 200, 75));
            g2.drawString(sabStatus, bx + 16, by + 44);
        } else if (localChar.getRole() == PlayerRole.GHOST) {
            g2.setColor(new Color(175, 205, 255));
            g2.drawString("GHOST MODE — Pass through walls, spectate players, and finish remaining tasks [E]!", bx + 16, by + 22);
            if (nearbyObj != null) {
                g2.setColor(new Color(255, 225, 70));
                g2.drawString(">> NEARBY: " + nearbyObj.getLabel() + " — Press [E]", bx + 16, by + 44);
            }
        } else {
            g2.setColor(new Color(140, 225, 255));
            g2.drawString("[C]Color [H]Hat [K]Skin [P]Pet  |  [F3] Debug HUD  |  [ESC] Main Menu" + reportHint, bx + 16, by + 22);

            if (nearbyObj != null) {
                g2.setColor(new Color(255, 225, 70));
                g2.drawString(">> NEARBY: " + nearbyObj.getLabel() + " (" + nearbyObj.getRoomId().getDisplayName() + ") — Press [E]",
                        bx + 16, by + 44);
            } else {
                g2.setColor(Color.LIGHT_GRAY);
                g2.drawString("Complete assigned tasks, check Admin/Security consoles, or report bodies [R]!", bx + 16, by + 44);
            }
        }
    }

    private void drawDebugHud(Graphics2D g2, GameEngine engine) {
        InputManager input = engine.getInputManager();
        Character c = engine.getLocalPlayer().getCharacter();
        String keys = input.getPressedKeysSnapshot().stream()
                .map(KeyEvent::getKeyText)
                .sorted()
                .collect(Collectors.joining(", "));
        if (keys.isEmpty()) keys = "None";

        int dx = engine.getConfig().getWindowWidth() - 390;
        int dy = 195;

        g2.setColor(new Color(10, 14, 24, 195));
        g2.fillRoundRect(dx, dy, 374, 86, 10, 10);
        g2.setColor(new Color(0, 210, 255, 160));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(dx, dy, 374, 86, 10, 10);

        g2.setFont(new Font("Consolas", Font.BOLD, 11));
        g2.setColor(new Color(80, 245, 150));
        g2.drawString(String.format("FPS: %d | QUALITY: %s | %s",
                engine.getCurrentFps(), engine.getConfig().getGraphicsQuality().getLabel(),
                engine.getNetworkManager().getStatusBanner()), dx + 12, dy + 22);
        g2.setColor(Color.WHITE);
        g2.drawString(String.format("POS: (%.0f, %.0f) | PLAYERS: %d", c.getX(), c.getY(), engine.getPlayers().size()), dx + 12, dy + 42);
        g2.drawString("KEYS: " + keys, dx + 12, dy + 62);
    }
}