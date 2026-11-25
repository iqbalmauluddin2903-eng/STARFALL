package starfall.gameplay;

import java.awt.Color;
import java.util.*;
import starfall.ai.AIController;
import starfall.core.GameConfig;
import starfall.entities.*;
import starfall.entities.Character;
import starfall.utils.Logger;

public class MeetingManager {

    public static class ChatMessage {
        private final String senderName;
        private final Color senderColor;
        private final String text;
        private final boolean systemMessage;

        public ChatMessage(String senderName, Color senderColor, String text, boolean systemMessage) {
            this.senderName = senderName;
            this.senderColor = senderColor;
            this.text = text;
            this.systemMessage = systemMessage;
        }

        public String getSenderName() { return senderName; }
        public Color getSenderColor() { return senderColor; }
        public String getText() { return text; }
        public boolean isSystemMessage() { return systemMessage; }
    }

    private final List<ChatMessage> chatHistory = new ArrayList<>();
    private final Set<String> aiHasSpokenThisMeeting = new HashSet<>();
    private final Random rng = new Random();

    private String headline = "";
    private String callerId = "";
    private int emergencyMeetingsRemaining = 2;
    private double emergencyCooldown = 0.0;

    private boolean discussionPhase = true;
    private double phaseTimer = 0.0;
    private double resultRevealTimer = 0.0;

    public void reset(GameConfig config) {
        chatHistory.clear();
        aiHasSpokenThisMeeting.clear();
        headline = "";
        callerId = "";
        emergencyMeetingsRemaining = config.getEmergencyMeetingsPerPlayer();
        emergencyCooldown = config.getEmergencyCooldownSeconds();
        discussionPhase = true;
        phaseTimer = 0.0;
        resultRevealTimer = 0.0;
    }

    public void updateCooldown(double dt) {
        if (emergencyCooldown > 0.0) {
            emergencyCooldown = Math.max(0.0, emergencyCooldown - dt);
        }
    }

    public boolean canCallEmergency(Character caller, SabotageManager sabotageManager) {
        if (caller == null || !caller.isAlive() || caller.getRole() == PlayerRole.GHOST) return false;
        if (emergencyMeetingsRemaining <= 0 || emergencyCooldown > 0.0) return false;
        return sabotageManager.getActiveSabotage() == SabotageType.NONE;
    }

    public Character findReportableBody(Character reporter, List<Player> players) {
        if (reporter == null || !reporter.isAlive() || reporter.getRole() == PlayerRole.GHOST) return null;

        Character best = null;
        double bestDist = reporter.getInteractionRange() * 1.25;

        for (Player p : players) {
            Character c = p.getCharacter();
            if (c.hasUnreportedBody() && !c.isBodyReported()) {
                double d = reporter.distanceTo(c.getDeadBodyX(), c.getDeadBodyY());
                if (d <= bestDist) {
                    bestDist = d;
                    best = c;
                }
            }
        }
        return best;
    }

    public void startNetworkMeeting(String callerId, String serverHeadline, List<Player> players,
                                    GameConfig config, SabotageManager sabotageManager, VotingManager votingManager) {
        this.callerId = callerId;
        this.headline = serverHeadline;
        sabotageManager.repairActiveSabotage();
        beginMeetingCommon(players, config, votingManager);
        addSystemMessage(serverHeadline);
    }

    public boolean startEmergencyMeeting(Player caller, List<Player> players, GameConfig config,
                                         SabotageManager sabotageManager, VotingManager votingManager) {
        if (!canCallEmergency(caller.getCharacter(), sabotageManager)) return false;

        emergencyMeetingsRemaining--;
        emergencyCooldown = config.getEmergencyCooldownSeconds();
        callerId = caller.getPlayerId();
        headline = "EMERGENCY MEETING CALLED BY " + caller.getCharacter().getName();

        beginMeetingCommon(players, config, votingManager);
        addSystemMessage(caller.getCharacter().getName() + " pressed the Emergency Button!");
        Logger.warn(headline);
        return true;
    }

    public boolean startBodyReportMeeting(Player reporter, Character deadBody, List<Player> players,
                                          GameConfig config, SabotageManager sabotageManager,
                                          VotingManager votingManager) {
        if (reporter == null || deadBody == null || !reporter.getCharacter().isAlive()) return false;

        deadBody.setBodyReported(true);
        deadBody.setHasUnreportedBody(false);
        callerId = reporter.getPlayerId();
        headline = "DEAD BODY REPORTED — " + deadBody.getName() + " FOUND IN " + deadBody.getCurrentRoom().getDisplayName();

        sabotageManager.repairActiveSabotage();

        beginMeetingCommon(players, config, votingManager);
        addSystemMessage(reporter.getCharacter().getName() + " reported " + deadBody.getName() + "'s body in "
                + deadBody.getCurrentRoom().getDisplayName() + "!");
        Logger.warn(headline);
        return true;
    }

    private void beginMeetingCommon(List<Player> players, GameConfig config, VotingManager votingManager) {
        chatHistory.clear();
        aiHasSpokenThisMeeting.clear();
        votingManager.reset();

        discussionPhase = config.getDiscussionTimeSeconds() > 0.0;
        phaseTimer = discussionPhase ? config.getDiscussionTimeSeconds() : config.getVotingTimeSeconds();
        resultRevealTimer = 0.0;

        gatherPlayersAtCafeteria(players);
    }

    public void gatherPlayersAtCafeteria(List<Player> players) {
        double centerX = 1630.0;
        double centerY = 360.0;
        int livingIdx = 0;
        int totalLiving = 0;
        for (Player p : players) {
            if (p.getCharacter().isAlive()) totalLiving++;
        }

        for (Player p : players) {
            Character c = p.getCharacter();
            if (c instanceof Impostor imp && imp.isInsideVent()) {
                imp.setInsideVent(false);
                imp.setCurrentVentId(null);
            }

            if (!c.isAlive()) {
                c.setBodyReported(true);
                c.setHasUnreportedBody(false);
                if (c.getRole() != PlayerRole.GHOST) {
                    c.becomeGhost();
                }
            } else {
                double angle = (livingIdx * Math.PI * 2.0) / Math.max(1, totalLiving);
                double sx = centerX + Math.cos(angle) * 145.0;
                double sy = centerY + Math.sin(angle) * 125.0;
                c.setX(sx);
                c.setY(sy);
                c.setVelocity(0, 0);
                c.setMovementState(MovementState.IDLE);
                livingIdx++;
            }
        }
    }

    public void updateNetworkTimers(double dt, GameConfig config) {
        if (phaseTimer > 0.0) {
            phaseTimer = Math.max(0.0, phaseTimer - dt);
            if (discussionPhase && phaseTimer <= 0.0) {
                discussionPhase = false;
                phaseTimer = config.getVotingTimeSeconds();
                addSystemMessage("Discussion ended. Voting is now OPEN!");
            }
        }
    }

    public boolean update(double dt, List<Player> players, List<AIController> aiControllers,
                          GameConfig config, VotingManager votingManager) {
        if (votingManager.isFinalized()) {
            resultRevealTimer -= dt;
            return resultRevealTimer <= 0.0;
        }

        phaseTimer -= dt;
        processAIBotMeetingActions(players, aiControllers, votingManager);

        if (discussionPhase) {
            if (phaseTimer <= 0.0) {
                discussionPhase = false;
                phaseTimer = config.getVotingTimeSeconds();
                addSystemMessage("Discussion ended. Voting is now OPEN!");
            }
            return false;
        }

        if (votingManager.allLivingPlayersVoted(players) || phaseTimer <= 0.0) {
            votingManager.finalizeVotes(players);
            resultRevealTimer = 4.0;
            addSystemMessage("Voting has ended! Tallying results...");
        }
        return false;
    }

    private void processAIBotMeetingActions(List<Player> players, List<AIController> aiControllers,
                                            VotingManager votingManager) {
        for (AIController ai : aiControllers) {
            Player bot = ai.getPlayer();
            Character bc = bot.getCharacter();
            if (!bc.isAlive() || !bc.isConnected()) continue;

            if (!aiHasSpokenThisMeeting.contains(bot.getPlayerId()) && rng.nextDouble() < 0.015) {
                aiHasSpokenThisMeeting.add(bot.getPlayerId());
                String statement = ai.generateMeetingStatement(this, players);
                addPlayerChat(bc, statement);
            }

            if (!discussionPhase && !votingManager.hasVoted(bot.getPlayerId()) && rng.nextDouble() < 0.04) {
                String targetId = ai.chooseVoteTarget(players, votingManager);
                votingManager.castVote(bot.getPlayerId(), targetId, players);
            }
        }
    }

    public void addPlayerChat(Character sender, String text) {
        if (sender == null || text == null || text.isBlank()) return;
        String clean = text.trim();
        if (clean.length() > 80) clean = clean.substring(0, 80);
        String prefix = sender.isAlive() ? sender.getName() : "[GHOST] " + sender.getName();
        chatHistory.add(new ChatMessage(prefix, sender.getColor().getMainColor(), clean, false));
        trimChatHistory();
    }

    public void addRawChatMessage(String senderDisplayName, Color color, String text) {
        chatHistory.add(new ChatMessage(senderDisplayName, color, text, false));
        trimChatHistory();
    }

    public void addSystemMessage(String text) {
        chatHistory.add(new ChatMessage("SYSTEM", new Color(255, 215, 80), text, true));
        trimChatHistory();
    }

    private void trimChatHistory() {
        while (chatHistory.size() > 14) {
            chatHistory.remove(0);
        }
    }

    public List<ChatMessage> getChatHistory() { return Collections.unmodifiableList(chatHistory); }
    public String getHeadline() { return headline; }
    public String getCallerId() { return callerId; }
    public int getEmergencyMeetingsRemaining() { return emergencyMeetingsRemaining; }
    public double getEmergencyCooldown() { return emergencyCooldown; }
    public boolean isDiscussionPhase() { return discussionPhase; }
    public double getPhaseTimer() { return phaseTimer; }
    public double getResultRevealTimer() { return resultRevealTimer; }
}