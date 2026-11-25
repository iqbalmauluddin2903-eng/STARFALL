package starfall.ui;

import starfall.core.InputManager;
import starfall.entities.Character;
import starfall.entities.MovementState;
import starfall.entities.Player;
import starfall.graphics.SpriteManager;
import starfall.map.GameMap;
import starfall.map.Room;
import starfall.map.RoomId;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class SurveillanceOverlay {

    public enum Mode {
        NONE,
        ADMIN_MAP,
        SECURITY_CAMS
    }

    private static final String[] CAM_NAMES = {
        "CAM 01: WEST CORRIDOR (MEDBAY / UPPER ENG)",
        "CAM 02: CENTRAL HUB (CAFETERIA / ADMIN)",
        "CAM 03: EAST SPINE (WEAPONS / O2 / NAV)",
        "CAM 04: REACTOR & SECURITY HALLWAY"
    };

    private static final Point[] CAM_CENTERS = {
        new Point(1100, 340),
        new Point(1630, 740),
        new Point(2425, 760),
        new Point(605, 880)
    };

    private final SpriteManager spriteManager = new SpriteManager();
    private Mode mode = Mode.NONE;
    private int activeCamIndex = 0;

    public void openAdminMap() {
        this.mode = Mode.ADMIN_MAP;
    }

    public void openSecurityCams() {
        this.mode = Mode.SECURITY_CAMS;
        this.activeCamIndex = 0;
    }

    public void close() {
        this.mode = Mode.NONE;
    }

    public boolean isOpen() {
        return mode != Mode.NONE;
    }

    public void update(InputManager input, int screenW, int screenH) {
        if (mode == Mode.NONE) return;

        if (input.consumeKeyPress(KeyEvent.VK_ESCAPE) || input.consumeKeyPress(KeyEvent.VK_E)) {
            close();
            return;
        }

        if (mode == Mode.SECURITY_CAMS) {
            if (input.consumeKeyPress(KeyEvent.VK_A) || input.consumeKeyPress(KeyEvent.VK_LEFT)) {
                activeCamIndex = Math.floorMod(activeCamIndex - 1, CAM_NAMES.length);
            } else if (input.consumeKeyPress(KeyEvent.VK_D) || input.consumeKeyPress(KeyEvent.VK_RIGHT)) {
                activeCamIndex = Math.floorMod(activeCamIndex + 1, CAM_NAMES.length);
            }
        }

        if (!input.consumeLeftClick()) return;
        Rectangle modal = getModalRect(screenW, screenH);
        int mx = input.getMouseX();
        int my = input.getMouseY();

        if (mode == Mode.SECURITY_CAMS) {
            Rectangle prevBtn = new Rectangle(modal.x + 30, modal.y + modal.height - 54, 150, 36);
            Rectangle nextBtn = new Rectangle(modal.x + modal.width - 180, modal.y + modal.height - 54, 150, 36);
            if (prevBtn.contains(mx, my)) {
                activeCamIndex = Math.floorMod(activeCamIndex - 1, CAM_NAMES.length);
                return;
            }
            if (nextBtn.contains(mx, my)) {
                activeCamIndex = Math.floorMod(activeCamIndex + 1, CAM_NAMES.length);
                return;
            }
        }

        Rectangle closeBtn = new Rectangle(modal.x + modal.width / 2 - 80, modal.y + modal.height - 54, 160, 36);
        if (closeBtn.contains(mx, my)) {
            close();
        }
    }

    public void render(Graphics2D g2, int screenW, int screenH, GameMap map, List<Player> players) {
        if (mode == Mode.NONE) return;

        g2.setColor(new Color(5, 8, 15, 190));
        g2.fillRect(0, 0, screenW, screenH);

        Rectangle modal = getModalRect(screenW, screenH);
        g2.setColor(new Color(18, 24, 38));
        g2.fillRoundRect(modal.x, modal.y, modal.width, modal.height, 18, 18);
        g2.setColor(new Color(0, 215, 255));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawRoundRect(modal.x, modal.y, modal.width, modal.height, 18, 18);

        if (mode == Mode.ADMIN_MAP) {
            renderAdminMap(g2, modal, map, players);
        } else if (mode == Mode.SECURITY_CAMS) {
            renderSecurityCam(g2, modal, map, players);
        }
    }

    private void renderAdminMap(Graphics2D g2, Rectangle modal, GameMap map, List<Player> players) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 22));
        g2.setColor(Color.WHITE);
        g2.drawString("ADMIN TABLE — LIVE CREW PRESENCE SENSOR", modal.x + 26, modal.y + 38);

        // Count living players in each non-corridor room
        Map<RoomId, Integer> roomCounts = new EnumMap<>(RoomId.class);
        for (Player p : players) {
            Character c = p.getCharacter();
            if (c.isAlive() && c.getMovementState() != MovementState.VENTING) {
                RoomId r = c.getCurrentRoom();
                if (r != RoomId.CORRIDOR) {
                    roomCounts.put(r, roomCounts.getOrDefault(r, 0) + 1);
                }
            }
        }

        int mapX = modal.x + 30;
        int mapY = modal.y + 56;
        int mapW = modal.width - 60;
        int mapH = modal.height - 125;
        double sx = mapW / (double) GameMap.WORLD_WIDTH;
        double sy = mapH / (double) GameMap.WORLD_HEIGHT;

        g2.setColor(new Color(10, 14, 24));
        g2.fillRoundRect(mapX, mapY, mapW, mapH, 12, 12);

        for (Room room : map.getRooms()) {
            Rectangle b = room.getBounds();
            int rx = mapX + (int) (b.x * sx);
            int ry = mapY + (int) (b.y * sy);
            int rw = Math.max(3, (int) (b.width * sx));
            int rh = Math.max(3, (int) (b.height * sy));

            g2.setColor(room.isCorridor() ? new Color(48, 56, 70) : new Color(75, 95, 128));
            g2.fillRect(rx, ry, rw, rh);

            if (!room.isCorridor()) {
                g2.setFont(new Font("Consolas", Font.BOLD, 10));
                g2.setColor(new Color(200, 225, 255));
                g2.drawString(room.getDisplayName(), rx + 6, ry + 14);

                int count = roomCounts.getOrDefault(room.getId(), 0);
                for (int i = 0; i < count; i++) {
                    g2.setColor(new Color(255, 215, 55));
                    g2.fillRoundRect(rx + 8 + (i % 4) * 16, ry + 20 + (i / 4) * 18, 11, 14, 6, 6);
                }
            }
        }

        drawCloseButton(g2, modal);
    }

    private void renderSecurityCam(Graphics2D g2, Rectangle modal, GameMap map, List<Player> players) {
        g2.setFont(new Font("Consolas", Font.BOLD, 18));
        g2.setColor(new Color(80, 245, 145));
        g2.drawString("● REC  |  " + CAM_NAMES[activeCamIndex], modal.x + 26, modal.y + 36);

        int viewX = modal.x + 26;
        int viewY = modal.y + 52;
        int viewW = modal.width - 52;
        int viewH = modal.height - 120;

        Point focus = CAM_CENTERS[activeCamIndex];
        double camX = focus.x - viewW / 2.0;
        double camY = focus.y - viewH / 2.0;

        Shape prevClip = g2.getClip();
        g2.setClip(viewX, viewY, viewW, viewH);

        g2.setColor(new Color(8, 12, 18));
        g2.fillRect(viewX, viewY, viewW, viewH);

        Graphics2D wg = (Graphics2D) g2.create();
        wg.translate(viewX - camX, viewY - camY);

        for (Room room : map.getRooms()) {
            Rectangle b = room.getBounds();
            wg.setColor(room.getFloorColor());
            wg.fillRect(b.x, b.y, b.width, b.height);
        }
        for (Player p : players) {
            Character c = p.getCharacter();
            spriteManager.drawDeadBodyAtCrimeScene(wg, c);
            if (c.isAlive()) {
                spriteManager.drawCharacter(wg, c, false);
            }
        }
        wg.dispose();

        // Subtle CCTV scanlines
        g2.setColor(new Color(0, 255, 130, 18));
        for (int y = viewY; y < viewY + viewH; y += 6) {
            g2.drawLine(viewX, y, viewX + viewW, y);
        }
        g2.setClip(prevClip);

        // Prev / Next camera buttons
        drawCamButton(g2, new Rectangle(modal.x + 30, modal.y + modal.height - 54, 150, 36), "< PREV CAM [A]");
        drawCamButton(g2, new Rectangle(modal.x + modal.width - 180, modal.y + modal.height - 54, 150, 36), "NEXT CAM [D] >");
        drawCloseButton(g2, modal);
    }

    private void drawCamButton(Graphics2D g2, Rectangle r, String text) {
        g2.setColor(new Color(45, 62, 92));
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        g2.setColor(new Color(0, 210, 255));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        g2.setFont(new Font("Consolas", Font.BOLD, 12));
        g2.setColor(Color.WHITE);
        g2.drawString(text, r.x + 22, r.y + 23);
    }

    private void drawCloseButton(Graphics2D g2, Rectangle modal) {
        Rectangle r = new Rectangle(modal.x + modal.width / 2 - 80, modal.y + modal.height - 54, 160, 36);
        g2.setColor(new Color(215, 60, 60));
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        g2.setFont(new Font("Consolas", Font.BOLD, 13));
        g2.setColor(Color.WHITE);
        g2.drawString("CLOSE [ESC/E]", r.x + 28, r.y + 23);
    }

    private Rectangle getModalRect(int screenW, int screenH) {
        int w = Math.min(920, screenW - 140);
        int h = Math.min(540, screenH - 110);
        return new Rectangle((screenW - w) / 2, (screenH - h) / 2, w, h);
    }
}