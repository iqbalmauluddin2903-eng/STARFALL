package starfall.ui;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;
import starfall.core.GameConfig;
import starfall.core.InputManager;
import starfall.entities.Character;
import starfall.entities.Player;
import starfall.entities.PlayerRole;
import starfall.gameplay.MeetingManager;
import starfall.gameplay.VotingManager;
import starfall.network.NetworkManager;

public class MeetingScreen {

    private final StringBuilder chatDraft = new StringBuilder();

    public void resetInput() {
        chatDraft.setLength(0);
    }

    public void update(InputManager input, int screenW, int screenH, List<Player> players,
                       Player localPlayer, GameConfig config, MeetingManager meetingMgr,
                       VotingManager votingMgr, NetworkManager networkMgr) {

        if (input.consumeKeyPress(KeyEvent.VK_F4)) {
            config.toggleAnonymousVoting();
        }

        for (char ch : input.drainTypedCharacters()) {
            if (ch == '\b') {
                if (chatDraft.length() > 0) {
                    chatDraft.deleteCharAt(chatDraft.length() - 1);
                }
            } else if (ch == '\n' || ch == '\r') {
                submitChatMessage(localPlayer, meetingMgr, networkMgr);
            } else if (ch >= 32 && ch < 127 && chatDraft.length() < 64) {
                chatDraft.append(ch);
            }
        }

        if (!input.consumeLeftClick()) return;

        int mx = input.getMouseX();
        int my = input.getMouseY();
        Rectangle modal = getModalRect(screenW, screenH);

        Rectangle sendBtn = new Rectangle(modal.x + modal.width - 105, modal.y + modal.height - 54, 82, 34);
        if (sendBtn.contains(mx, my)) {
            submitChatMessage(localPlayer, meetingMgr, networkMgr);
            return;
        }

        Character localChar = localPlayer.getCharacter();
        String localId = localPlayer.getPlayerId();
        if (meetingMgr.isDiscussionPhase() || votingMgr.isFinalized() || !localChar.isAlive() || votingMgr.hasVoted(localId)) {
            return;
        }

        int cardW = 315;
        int cardH = 68;
        int startX = modal.x + 24;
        int startY = modal.y + 82;

        for (int i = 0; i < players.size(); i++) {
            Player target = players.get(i);
            Character tc = target.getCharacter();
            if (!tc.isAlive()) continue;

            int col = i % 2;
            int row = i / 2;
            int cx = startX + col * (cardW + 16);
            int cy = startY + row * (cardH + 12);
            Rectangle cardRect = new Rectangle(cx, cy, cardW, cardH);

            if (cardRect.contains(mx, my)) {
                if (target.getPlayerId().equals(votingMgr.getPendingTargetId())) {
                    Rectangle confirmBtn = new Rectangle(cx + cardW - 76, cy + 16, 32, 36);
                    Rectangle cancelBtn = new Rectangle(cx + cardW - 38, cy + 16, 32, 36);
                    if (confirmBtn.contains(mx, my)) {
                        if (networkMgr != null && networkMgr.isConnected()) {
                            networkMgr.sendVote(target.getPlayerId());
                        } else {
                            votingMgr.castVote(localId, target.getPlayerId(), players);
                        }
                    } else if (cancelBtn.contains(mx, my)) {
                        votingMgr.clearPendingTarget();
                    }
                } else {
                    votingMgr.selectPendingTarget(target.getPlayerId());
                }
                return;
            }
        }

        Rectangle skipBtn = new Rectangle(startX, modal.y + modal.height - 56, 175, 38);
        if (skipBtn.contains(mx, my)) {
            if (VotingManager.SKIP_VOTE.equals(votingMgr.getPendingTargetId())) {
                if (networkMgr != null && networkMgr.isConnected()) {
                    networkMgr.sendVote(VotingManager.SKIP_VOTE);
                } else {
                    votingMgr.castVote(localId, VotingManager.SKIP_VOTE, players);
                }
            } else {
                votingMgr.selectPendingTarget(VotingManager.SKIP_VOTE);
            }
        }
    }

    private void submitChatMessage(Player localPlayer, MeetingManager meetingMgr, NetworkManager networkMgr) {
        if (chatDraft.length() > 0) {
            String msg = chatDraft.toString();
            chatDraft.setLength(0);
            if (networkMgr != null && networkMgr.isConnected()) {
                networkMgr.sendChatMessage(localPlayer.getCharacter(), msg);
            } else {
                meetingMgr.addPlayerChat(localPlayer.getCharacter(), msg);
            }
        }
    }

    public void render(Graphics2D g2, int screenW, int screenH, List<Player> players,
                       Player localPlayer, GameConfig config, MeetingManager meetingMgr, VotingManager votingMgr) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(6, 9, 16, 215));
        g2.fillRect(0, 0, screenW, screenH);

        Rectangle modal = getModalRect(screenW, screenH);
        g2.setColor(new Color(20, 26, 40));
        g2.fillRoundRect(modal.x, modal.y, modal.width, modal.height, 18, 18);
        g2.setColor(new Color(0, 215, 255));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawRoundRect(modal.x, modal.y, modal.width, modal.height, 18, 18);

        g2.setColor(new Color(13, 17, 28));
        g2.fillRoundRect(modal.x + 6, modal.y + 6, modal.width - 12, 58, 14, 14);

        g2.setFont(new Font("SansSerif", Font.BOLD, 19));
        g2.setColor(new Color(255, 85, 85));
        g2.drawString(meetingMgr.getHeadline(), modal.x + 22, modal.y + 34);

        String phaseText;
        if (votingMgr.isFinalized()) {
            phaseText = "RESULTS TALLIED";
        } else if (meetingMgr.isDiscussionPhase()) {
            phaseText = String.format("DISCUSSION: %.0fs", Math.ceil(meetingMgr.getPhaseTimer()));
        } else {
            phaseText = String.format("VOTING ENDS: %.0fs", Math.ceil(meetingMgr.getPhaseTimer()));
        }

        g2.setFont(new Font("Consolas", Font.BOLD, 15));
        g2.setColor(new Color(255, 220, 80));
        g2.drawString(phaseText, modal.x + modal.width - 195, modal.y + 32);

        g2.setFont(new Font("Consolas", Font.PLAIN, 11));
        g2.setColor(new Color(150, 200, 240));
        g2.drawString("Anonymous Votes [F4]: " + (config.isAnonymousVoting() ? "ON" : "OFF"), modal.x + 22, modal.y + 54);

        drawPlayerCards(g2, modal, players, localPlayer, config, meetingMgr, votingMgr);
        drawChatPanel(g2, modal, meetingMgr);
    }

    private void drawPlayerCards(Graphics2D g2, Rectangle modal, List<Player> players,
                                 Player localPlayer, GameConfig config,
                                 MeetingManager meetingMgr, VotingManager votingMgr) {
        int cardW = 315;
        int cardH = 68;
        int startX = modal.x + 24;
        int startY = modal.y + 82;
        String localId = localPlayer.getPlayerId();

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            Character c = p.getCharacter();
            int col = i % 2;
            int row = i / 2;
            int cx = startX + col * (cardW + 16);
            int cy = startY + row * (cardH + 12);

            boolean isPending = p.getPlayerId().equals(votingMgr.getPendingTargetId());
            g2.setColor(!c.isAlive() ? new Color(28, 30, 38)
                    : (isPending ? new Color(45, 65, 95) : new Color(32, 40, 58)));
            g2.fillRoundRect(cx, cy, cardW, cardH, 12, 12);

            g2.setColor(isPending ? new Color(255, 220, 60) : new Color(75, 92, 122));
            g2.setStroke(new BasicStroke(isPending ? 2.5f : 1.5f));
            g2.drawRoundRect(cx, cy, cardW, cardH, 12, 12);

            g2.setColor(c.getColor().getMainColor());
            g2.fillRoundRect(cx + 12, cy + 14, 28, 38, 16, 16);
            g2.setColor(new Color(165, 228, 252));
            g2.fillRoundRect(cx + 22, cy + 20, 16, 10, 6, 6);

            g2.setFont(new Font("Consolas", Font.BOLD, 14));
            boolean showRedName = localPlayer.getCharacter().getRole() == PlayerRole.IMPOSTOR
                    && c.getRole() == PlayerRole.IMPOSTOR;
            g2.setColor(!c.isAlive() ? Color.GRAY : (showRedName ? new Color(255, 80, 80) : Color.WHITE));
            String nameLabel = c.getName() + (p.isLocalPlayer() ? " (YOU)" : "");
            g2.drawString(nameLabel, cx + 52, cy + 28);

            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            if (!c.isAlive()) {
                g2.setColor(new Color(220, 75, 75));
                g2.drawString("DECEASED", cx + 52, cy + 46);
            } else if (votingMgr.hasVoted(p.getPlayerId())) {
                g2.setColor(new Color(80, 240, 140));
                g2.drawString("I VOTED", cx + 52, cy + 46);
            } else if (p.getPlayerId().equals(meetingMgr.getCallerId())) {
                g2.setColor(new Color(255, 200, 70));
                g2.drawString("REPORTER", cx + 52, cy + 46);
            }

            if (isPending && !votingMgr.isFinalized() && localPlayer.getCharacter().isAlive() && !votingMgr.hasVoted(localId)) {
                g2.setColor(new Color(46, 213, 115));
                g2.fillRoundRect(cx + cardW - 76, cy + 16, 32, 36, 8, 8);
                g2.setColor(Color.BLACK);
                g2.drawString("OK", cx + cardW - 68, cy + 39);

                g2.setColor(new Color(235, 65, 65));
                g2.fillRoundRect(cx + cardW - 38, cy + 16, 32, 36, 8, 8);
                g2.setColor(Color.WHITE);
                g2.drawString("X", cx + cardW - 26, cy + 39);
            }

            if (votingMgr.isFinalized()) {
                List<Player> voters = votingMgr.getVotersFor(p.getPlayerId(), players);
                for (int v = 0; v < voters.size(); v++) {
                    Color pipCol = config.isAnonymousVoting()
                            ? new Color(170, 180, 195)
                            : voters.get(v).getCharacter().getColor().getMainColor();
                    g2.setColor(pipCol);
                    g2.fillOval(cx + 52 + v * 18, cy + 48, 13, 13);
                    g2.setColor(Color.BLACK);
                    g2.drawOval(cx + 52 + v * 18, cy + 48, 13, 13);
                }
            }
        }

        Rectangle skipBtn = new Rectangle(startX, modal.y + modal.height - 56, 175, 38);
        boolean skipPending = VotingManager.SKIP_VOTE.equals(votingMgr.getPendingTargetId());
        g2.setColor(skipPending ? new Color(255, 205, 50) : new Color(65, 75, 95));
        g2.fillRoundRect(skipBtn.x, skipBtn.y, skipBtn.width, skipBtn.height, 10, 10);
        g2.setFont(new Font("Consolas", Font.BOLD, 13));
        g2.setColor(skipPending ? Color.BLACK : Color.WHITE);
        g2.drawString(skipPending ? "CONFIRM SKIP?" : "SKIP VOTE", skipBtn.x + 28, skipBtn.y + 24);

        if (votingMgr.isFinalized()) {
            List<Player> skipVoters = votingMgr.getVotersFor(VotingManager.SKIP_VOTE, players);
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawString("Skipped: " + skipVoters.size(), skipBtn.x + 190, skipBtn.y + 24);
        }
    }

    private void drawChatPanel(Graphics2D g2, Rectangle modal, MeetingManager meetingMgr) {
        int chatX = modal.x + 685;
        int chatY = modal.y + 82;
        int chatW = modal.width - 708;
        int chatH = modal.height - 150;

        g2.setColor(new Color(14, 18, 28));
        g2.fillRoundRect(chatX, chatY, chatW, chatH, 12, 12);
        g2.setColor(new Color(65, 82, 112));
        g2.drawRoundRect(chatX, chatY, chatW, chatH, 12, 12);

        g2.setFont(new Font("Consolas", Font.BOLD, 13));
        g2.setColor(new Color(140, 215, 255));
        g2.drawString("DISCUSSION CHAT", chatX + 14, chatY + 22);

        int msgY = chatY + 46;
        for (MeetingManager.ChatMessage msg : meetingMgr.getChatHistory()) {
            g2.setFont(new Font("Consolas", Font.BOLD, 12));
            g2.setColor(msg.getSenderColor());
            g2.drawString(msg.getSenderName() + ":", chatX + 14, msgY);

            g2.setFont(new Font("Consolas", Font.PLAIN, 12));
            g2.setColor(msg.isSystemMessage() ? new Color(255, 225, 130) : Color.WHITE);
            g2.drawString(msg.getText(), chatX + 14, msgY + 15);
            msgY += 34;
            if (msgY > chatY + chatH - 18) break;
        }

        int inputY = modal.y + modal.height - 54;
        int inputW = chatW - 92;
        g2.setColor(new Color(28, 34, 48));
        g2.fillRoundRect(chatX, inputY, inputW, 34, 8, 8);
        g2.setColor(new Color(0, 200, 240));
        g2.drawRoundRect(chatX, inputY, inputW, 34, 8, 8);

        g2.setFont(new Font("Consolas", Font.PLAIN, 12));
        g2.setColor(Color.WHITE);
        String displayDraft = chatDraft.isEmpty() ? "Type message & press ENTER..." : chatDraft + "_";
        if (chatDraft.isEmpty()) g2.setColor(Color.GRAY);
        g2.drawString(displayDraft, chatX + 10, inputY + 22);

        Rectangle sendBtn = new Rectangle(modal.x + modal.width - 105, inputY, 82, 34);
        g2.setColor(new Color(0, 205, 245));
        g2.fillRoundRect(sendBtn.x, sendBtn.y, sendBtn.width, sendBtn.height, 8, 8);
        g2.setFont(new Font("Consolas", Font.BOLD, 12));
        g2.setColor(Color.BLACK);
        g2.drawString("SEND", sendBtn.x + 24, sendBtn.y + 22);
    }

    private Rectangle getModalRect(int screenW, int screenH) {
        int w = Math.min(1140, screenW - 80);
        int h = Math.min(600, screenH - 70);
        return new Rectangle((screenW - w) / 2, (screenH - h) / 2, w, h);
    }
}