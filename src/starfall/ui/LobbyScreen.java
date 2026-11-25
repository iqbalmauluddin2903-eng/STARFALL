package starfall.ui;

import java.awt.*;
import java.util.List;
import starfall.core.GameConfig;
import starfall.core.GameEngine;
import starfall.core.GameState;
import starfall.core.InputManager;
import starfall.entities.Character;
import starfall.entities.Player;
import starfall.gameplay.MeetingManager;
import starfall.graphics.SpriteManager;
import starfall.network.NetworkManager;

public class LobbyScreen {

    private enum FocusField {
        NONE,
        PLAYER_NAME,
        JOIN_IP_PORT,
        JOIN_CODE,
        LOBBY_CHAT
    }

    private final SpriteManager spriteManager = new SpriteManager();
    private final StringBuilder nameDraft = new StringBuilder("CAPTAIN");
    private final StringBuilder ipPortDraft = new StringBuilder("127.0.0.1:25565");
    private final StringBuilder roomCodeDraft = new StringBuilder("");
    private final StringBuilder chatDraft = new StringBuilder();

    private FocusField focused = FocusField.NONE;
    private double animTime = 0.0;

    public void onEnter(Character localChar, GameConfig config) {
        nameDraft.setLength(0);
        nameDraft.append(localChar.getName());
        ipPortDraft.setLength(0);
        ipPortDraft.append(config.getDefaultHostIp()).append(':').append(config.getServerPort());
        chatDraft.setLength(0);
        focused = FocusField.NONE;
    }

    public void update(double dt, InputManager input, int w, int h, GameEngine engine) {
        animTime += dt;
        Character localChar = engine.getLocalPlayer().getCharacter();
        localChar.update(dt, null);

        for (char ch : input.drainTypedCharacters()) {
            if (ch == '\b') {
                deleteLastChar();
            } else if (ch == '\n' || ch == '\r') {
                handleEnterKey(engine);
            } else if (ch >= 32 && ch < 127) {
                appendTypedChar(ch, engine);
            }
        }

        if (!input.consumeLeftClick()) return;

        int mx = input.getMouseX();
        int my = input.getMouseY();

        Rectangle nameBox = new Rectangle(420, 145, 240, 34);
        Rectangle ipBox = new Rectangle(420, 445, 240, 34);
        Rectangle codeBox = new Rectangle(420, 505, 240, 34);
        Rectangle chatBox = new Rectangle(855, h - 115, 285, 34);

        if (nameBox.contains(mx, my)) { focused = FocusField.PLAYER_NAME; return; }
        if (ipBox.contains(mx, my)) { focused = FocusField.JOIN_IP_PORT; return; }
        if (codeBox.contains(mx, my)) { focused = FocusField.JOIN_CODE; return; }
        if (chatBox.contains(mx, my)) { focused = FocusField.LOBBY_CHAT; return; }
        focused = FocusField.NONE;

        NetworkManager net = engine.getNetworkManager();

        // Customization buttons
        if (new Rectangle(420, 195, 115, 36).contains(mx, my)) {
            engine.getLocalPlayer().cycleColor();
            net.sendCustomization(localChar);
            return;
        }
        if (new Rectangle(545, 195, 115, 36).contains(mx, my)) {
            engine.getLocalPlayer().cycleHat();
            net.sendCustomization(localChar);
            return;
        }
        if (new Rectangle(420, 240, 115, 36).contains(mx, my)) {
            engine.getLocalPlayer().cycleSkin();
            net.sendCustomization(localChar);
            return;
        }
        if (new Rectangle(545, 240, 115, 36).contains(mx, my)) {
            engine.getLocalPlayer().cyclePet();
            net.sendCustomization(localChar);
            return;
        }

        // Host / Start Match button
        Rectangle hostBtn = new Rectangle(420, 315, 240, 40);
        if (hostBtn.contains(mx, my)) {
            if (!net.isConnected()) {
                net.hostGame(engine.getConfig(), localChar, engine.getConfig().isPrivateRoom());
            } else if (net.isLocalHost()) {
                net.requestStartMatch();
            }
            return;
        }

        // Impostor count & Private Room toggles (Host only)
        if (net.isConnected() && net.isLocalHost()) {
            if (new Rectangle(420, 365, 115, 34).contains(mx, my)) {
                int nextImp = (engine.getConfig().getImpostorCount() % 2) + 1;
                engine.getConfig().setImpostorCount(nextImp);
                net.sendLobbyConfig(engine.getConfig().isPrivateRoom(), nextImp);
                return;
            }
            if (new Rectangle(545, 365, 115, 34).contains(mx, my)) {
                boolean nextPriv = !engine.getConfig().isPrivateRoom();
                engine.getConfig().setPrivateRoom(nextPriv);
                net.sendLobbyConfig(nextPriv, engine.getConfig().getImpostorCount());
                return;
            }
        }

        // Join Room button
        Rectangle joinBtn = new Rectangle(420, 552, 240, 40);
        if (joinBtn.contains(mx, my)) {
            triggerJoin(engine);
            return;
        }

        // Chat Send button
        Rectangle sendBtn = new Rectangle(1148, h - 115, 76, 34);
        if (sendBtn.contains(mx, my)) {
            sendLobbyChat(engine);
            return;
        }

        // Leave Lobby / Offline Play button
        Rectangle offlineBtn = new Rectangle(w - 255, 22, 225, 38);
        if (offlineBtn.contains(mx, my)) {
            net.disconnect();
            engine.startNewMatch(false);
            engine.setState(GameState.PLAYING);
        }
    }

    private void deleteLastChar() {
        switch (focused) {
            case PLAYER_NAME -> { if (nameDraft.length() > 0) nameDraft.deleteCharAt(nameDraft.length() - 1); }
            case JOIN_IP_PORT -> { if (ipPortDraft.length() > 0) ipPortDraft.deleteCharAt(ipPortDraft.length() - 1); }
            case JOIN_CODE -> { if (roomCodeDraft.length() > 0) roomCodeDraft.deleteCharAt(roomCodeDraft.length() - 1); }
            case LOBBY_CHAT -> { if (chatDraft.length() > 0) chatDraft.deleteCharAt(chatDraft.length() - 1); }
            default -> {}
        }
    }

    private void appendTypedChar(char ch, GameEngine engine) {
        switch (focused) {
            case PLAYER_NAME -> {
                if (nameDraft.length() < 14) {
                    nameDraft.append(java.lang.Character.toUpperCase(ch));
                    Character lc = engine.getLocalPlayer().getCharacter();
                    lc.setName(nameDraft.toString());
                    engine.getNetworkManager().sendCustomization(lc);
                }
            }
            case JOIN_IP_PORT -> {
                if (ipPortDraft.length() < 26) ipPortDraft.append(ch);
            }
            case JOIN_CODE -> {
                if (roomCodeDraft.length() < 6) {
                    roomCodeDraft.append(java.lang.Character.toUpperCase(ch));
                }
            }
            case LOBBY_CHAT -> {
                if (chatDraft.length() < 56) chatDraft.append(ch);
            }
            default -> {}
        }
    }

    private void handleEnterKey(GameEngine engine) {
        if (focused == FocusField.LOBBY_CHAT) {
            sendLobbyChat(engine);
        } else if (focused == FocusField.JOIN_IP_PORT || focused == FocusField.JOIN_CODE) {
            triggerJoin(engine);
        } else if (focused == FocusField.PLAYER_NAME) {
            focused = FocusField.NONE;
        }
    }

    private void triggerJoin(GameEngine engine) {
        String raw = ipPortDraft.toString().trim();
        String ip = raw;
        int port = engine.getConfig().getServerPort();
        if (raw.contains(":")) {
            String[] parts = raw.split(":", 2);
            ip = parts[0].trim();
            try {
                port = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ignored) {}
        }
        engine.getNetworkManager().joinGame(ip, port, roomCodeDraft.toString(), engine.getLocalPlayer().getCharacter());
    }

    private void sendLobbyChat(GameEngine engine) {
        if (chatDraft.isEmpty()) return;
        String msg = chatDraft.toString();
        chatDraft.setLength(0);
        NetworkManager net = engine.getNetworkManager();
        if (net.isConnected()) {
            net.sendChatMessage(engine.getLocalPlayer().getCharacter(), msg);
        } else {
            engine.getMeetingManager().addPlayerChat(engine.getLocalPlayer().getCharacter(), msg);
        }
    }

    public void render(Graphics2D g2, int w, int h, GameEngine engine) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setPaint(new GradientPaint(0, 0, new Color(8, 12, 24), 0, h, new Color(18, 24, 42)));
        g2.fillRect(0, 0, w, h);

        NetworkManager net = engine.getNetworkManager();

        // Top Header Bar
        g2.setColor(new Color(14, 19, 32));
        g2.fillRect(0, 0, w, 78);
        g2.setColor(new Color(0, 210, 255));
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(0, 78, w, 78);

        g2.setFont(new Font("SansSerif", Font.BOLD, 26));
        g2.setColor(Color.WHITE);
        g2.drawString("STARFALL — MULTIPLAYER LOBBY", 28, 38);

        g2.setFont(new Font("Consolas", Font.BOLD, 14));
        g2.setColor(new Color(80, 245, 150));
        g2.drawString("STATUS: " + net.getStatusBanner(), 28, 64);

        if (!net.getRoomCode().isEmpty()) {
            g2.setColor(new Color(32, 42, 66));
            g2.fillRoundRect(w - 510, 18, 235, 44, 12, 12);
            g2.setColor(new Color(255, 215, 65));
            g2.drawRoundRect(w - 510, 18, 235, 44, 12, 12);
            g2.setFont(new Font("Consolas", Font.BOLD, 18));
            g2.drawString("ROOM CODE: " + net.getRoomCode(), w - 494, 46);
        }

        drawButton(g2, new Rectangle(w - 255, 22, 225, 38), "OFFLINE PLAY [ESC]", new Color(220, 65, 65), Color.WHITE);

        drawRosterPanel(g2, 26, 98, 360, h - 140, engine.getPlayers(), net);
        drawCenterControlPanel(g2, 402, 98, 420, h - 140, engine);
        drawChatPanel(g2, 838, 98, w - 864, h - 140, engine.getMeetingManager());
    }

    private void drawRosterPanel(Graphics2D g2, int x, int y, int w, int h, List<Player> players, NetworkManager net) {
        drawCardContainer(g2, x, y, w, h, "CREW MANIFEST (" + players.size() + "/8)");

        int rowY = y + 48;
        for (Player p : players) {
            Character c = p.getCharacter();
            g2.setColor(new Color(28, 36, 54));
            g2.fillRoundRect(x + 14, rowY, w - 28, 56, 10, 10);
            g2.setColor(p.isLocalPlayer() ? new Color(0, 225, 255) : new Color(65, 82, 112));
            g2.drawRoundRect(x + 14, rowY, w - 28, 56, 10, 10);

            g2.setColor(c.getColor().getMainColor());
            g2.fillRoundRect(x + 26, rowY + 10, 24, 34, 14, 14);
            g2.setColor(new Color(165, 228, 252));
            g2.fillRoundRect(x + 34, rowY + 15, 14, 9, 5, 5);

            g2.setFont(new Font("Consolas", Font.BOLD, 14));
            g2.setColor(Color.WHITE);
            g2.drawString(c.getName() + (p.isLocalPlayer() ? " (YOU)" : ""), x + 62, rowY + 26);

            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            boolean isHost = p.getPlayerId().equals(net.getHostPlayerId());
            if (isHost) {
                g2.setColor(new Color(255, 215, 65));
                g2.drawString("[HOST]", x + 62, rowY + 44);
            } else {
                g2.setColor(c.isConnected() ? new Color(80, 240, 140) : new Color(255, 95, 95));
                g2.drawString(c.isConnected() ? "READY" : "RECONNECTING...", x + 62, rowY + 44);
            }

            rowY += 64;
        }
    }

    private void drawCenterControlPanel(Graphics2D g2, int x, int y, int w, int h, GameEngine engine) {
        drawCardContainer(g2, x, y, w, h, "CUSTOMIZE & MATCH SETUP");

        Character localChar = engine.getLocalPlayer().getCharacter();
        NetworkManager net = engine.getNetworkManager();

        drawTextField(g2, new Rectangle(x + 18, y + 47, 240, 34), nameDraft.toString(),
                "Your Name", focused == FocusField.PLAYER_NAME);

        Graphics2D pg = (Graphics2D) g2.create();
        pg.translate(x + 335, y + 115 + Math.sin(animTime * 3.0) * 3.0);
        pg.scale(1.65, 1.65);
        Character previewClone = new starfall.entities.Crewmate("prev", "", 0, 0, localChar.getColor());
        previewClone.setHat(localChar.getHat());
        previewClone.setSkin(localChar.getSkin());
        spriteManager.drawCharacter(pg, previewClone, false);
        pg.dispose();

        drawButton(g2, new Rectangle(x + 18, y + 97, 115, 36), "COLOR [C]", localChar.getColor().getMainColor(), Color.WHITE);
        drawButton(g2, new Rectangle(x + 143, y + 97, 115, 36), "HAT [H]", new Color(55, 70, 98), Color.WHITE);
        drawButton(g2, new Rectangle(x + 18, y + 142, 115, 36), "SKIN [K]", new Color(55, 70, 98), Color.WHITE);
        drawButton(g2, new Rectangle(x + 143, y + 142, 115, 36), "PET [P]", new Color(55, 70, 98), Color.WHITE);

        if (!net.isConnected()) {
            drawButton(g2, new Rectangle(x + 18, y + 217, 240, 40), "HOST PRIVATE ROOM", new Color(46, 213, 115), Color.BLACK);
        } else if (net.isLocalHost()) {
            drawButton(g2, new Rectangle(x + 18, y + 217, 240, 40), "START MATCH", new Color(46, 213, 115), Color.BLACK);
            drawButton(g2, new Rectangle(x + 18, y + 267, 115, 34),
                    "IMP: " + engine.getConfig().getImpostorCount(), new Color(195, 55, 55), Color.WHITE);
            drawButton(g2, new Rectangle(x + 143, y + 267, 115, 34),
                    engine.getConfig().isPrivateRoom() ? "PRIVATE" : "PUBLIC", new Color(65, 95, 145), Color.WHITE);
        } else {
            g2.setFont(new Font("Consolas", Font.BOLD, 13));
            g2.setColor(new Color(255, 220, 90));
            g2.drawString("Waiting for Host to start match...", x + 20, y + 242);
        }

        g2.setColor(new Color(70, 88, 120));
        g2.drawLine(x + 16, y + 322, x + w - 16, y + 322);

        g2.setFont(new Font("Consolas", Font.BOLD, 12));
        g2.setColor(new Color(150, 215, 255));
        g2.drawString("JOIN LAN / ONLINE SERVER:", x + 18, y + 340);

        drawTextField(g2, new Rectangle(x + 18, y + 347, 240, 34), ipPortDraft.toString(),
                "IP:Port (e.g. 127.0.0.1:25565)", focused == FocusField.JOIN_IP_PORT);
        drawTextField(g2, new Rectangle(x + 18, y + 407, 240, 34), roomCodeDraft.toString(),
                "6-Char Room Code", focused == FocusField.JOIN_CODE);

        drawButton(g2, new Rectangle(x + 18, y + 454, 240, 40), "JOIN ROOM", new Color(0, 200, 245), Color.BLACK);
    }

    private void drawChatPanel(Graphics2D g2, int x, int y, int w, int h, MeetingManager meetingMgr) {
        drawCardContainer(g2, x, y, w, h, "LOBBY CHAT");

        int msgY = y + 52;
        for (MeetingManager.ChatMessage msg : meetingMgr.getChatHistory()) {
            g2.setFont(new Font("Consolas", Font.BOLD, 12));
            g2.setColor(msg.getSenderColor());
            g2.drawString(msg.getSenderName() + ":", x + 16, msgY);

            g2.setFont(new Font("Consolas", Font.PLAIN, 12));
            g2.setColor(msg.isSystemMessage() ? new Color(255, 225, 120) : Color.WHITE);
            g2.drawString(msg.getText(), x + 16, msgY + 15);
            msgY += 34;
            if (msgY > y + h - 75) break;
        }

        drawTextField(g2, new Rectangle(x + 17, y + h - 55, w - 115, 34), chatDraft.toString(),
                "Say hello...", focused == FocusField.LOBBY_CHAT);
        drawButton(g2, new Rectangle(x + w - 90, y + h - 55, 76, 34), "SEND", new Color(0, 205, 245), Color.BLACK);
    }

    private void drawCardContainer(Graphics2D g2, int x, int y, int w, int h, String title) {
        g2.setColor(new Color(16, 22, 36, 230));
        g2.fillRoundRect(x, y, w, h, 14, 14);
        g2.setColor(new Color(65, 88, 125));
        g2.setStroke(new BasicStroke(1.8f));
        g2.drawRoundRect(x, y, w, h, 14, 14);

        g2.setFont(new Font("Consolas", Font.BOLD, 14));
        g2.setColor(new Color(0, 220, 255));
        g2.drawString(title, x + 16, y + 26);
    }

    private void drawTextField(Graphics2D g2, Rectangle rect, String value, String placeholder, boolean active) {
        g2.setColor(new Color(26, 33, 50));
        g2.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 8, 8);
        g2.setColor(active ? new Color(255, 220, 65) : new Color(80, 100, 135));
        g2.setStroke(new BasicStroke(active ? 2f : 1.5f));
        g2.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 8, 8);

        g2.setFont(new Font("Consolas", Font.BOLD, 12));
        if (value.isEmpty()) {
            g2.setColor(Color.GRAY);
            g2.drawString(active ? "_" : placeholder, rect.x + 10, rect.y + 22);
        } else {
            g2.setColor(Color.WHITE);
            g2.drawString(active ? value + "_" : value, rect.x + 10, rect.y + 22);
        }
    }

    private void drawButton(Graphics2D g2, Rectangle rect, String text, Color bg, Color fg) {
        g2.setColor(bg);
        g2.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 9, 9);
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 9, 9);

        g2.setFont(new Font("Consolas", Font.BOLD, 12));
        int tw = g2.getFontMetrics().stringWidth(text);
        g2.setColor(fg);
        g2.drawString(text, rect.x + (rect.width - tw) / 2, rect.y + rect.height / 2 + 4);
    }
}