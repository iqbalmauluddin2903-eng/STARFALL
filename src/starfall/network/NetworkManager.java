package starfall.network;

import starfall.core.GameConfig;
import starfall.core.GameEngine;
import starfall.core.GameState;
import starfall.entities.*;
import starfall.entities.Character;
import starfall.gameplay.*;
import starfall.map.Door;
import starfall.utils.Logger;

import java.awt.Color;
import java.util.*;

public class NetworkManager {

    private final String localClientId = "net_" + UUID.randomUUID().toString().substring(0, 8);
    private GameServer hostedServer = null;
    private final GameClient client = new GameClient();

    private String roomCode = "";
    private String hostPlayerId = "";
    private String connectedHostIp = "127.0.0.1";
    private int connectedPort = 25565;
    private String statusBanner = "Offline Mode";

    private double moveSyncTimer = 0.0;
    private double pingTimer = 0.0;

    public boolean hostGame(GameConfig config, Character localChar, boolean isPrivate) {
        disconnect();
        try {
            hostedServer = new GameServer(config.getServerPort(), isPrivate);
            hostedServer.start();
            this.roomCode = hostedServer.getRoomCode();
            this.connectedHostIp = "127.0.0.1";
            this.connectedPort = config.getServerPort();

            NetworkMessage hello = buildHelloMessage(localChar, roomCode);
            client.connectAsync("127.0.0.1", config.getServerPort(), hello);
            statusBanner = "Hosting Room [" + roomCode + "] on port " + config.getServerPort();
            return true;
        } catch (Exception e) {
            Logger.error("Failed to host GameServer", e);
            statusBanner = "Host failed: " + e.getMessage();
            hostedServer = null;
            return false;
        }
    }

    public void joinGame(String ip, int port, String code, Character localChar) {
        disconnect();
        this.connectedHostIp = (ip == null || ip.isBlank()) ? "127.0.0.1" : ip.trim();
        this.connectedPort = port;
        this.roomCode = (code == null) ? "" : code.trim().toUpperCase();

        NetworkMessage hello = buildHelloMessage(localChar, this.roomCode);
        client.connectAsync(this.connectedHostIp, this.connectedPort, hello);
        statusBanner = "Connecting to " + connectedHostIp + ":" + connectedPort + "...";
    }

    public void reconnect(Character localChar) {
        if (connectedHostIp != null && !connectedHostIp.isBlank()) {
            joinGame(connectedHostIp, connectedPort, roomCode, localChar);
        }
    }

    public void disconnect() {
        client.disconnect();
        if (hostedServer != null) {
            hostedServer.stop();
            hostedServer = null;
        }
        roomCode = "";
        hostPlayerId = "";
        statusBanner = "Offline Mode";
    }

    private NetworkMessage buildHelloMessage(Character c, String code) {
        return new NetworkMessage(
                NetworkMessage.Type.HELLO,
                localClientId,
                code,
                c.getName(),
                c.getColor().name(),
                c.getHat().name(),
                c.getSkin().name(),
                c.getPet().name()
        );
    }

    public void sendCustomization(Character c) {
        if (!client.isConnected()) return;
        client.send(new NetworkMessage(
                NetworkMessage.Type.CUSTOMIZE,
                c.getName(),
                c.getColor().name(),
                c.getHat().name(),
                c.getSkin().name(),
                c.getPet().name()
        ));
    }

    public void sendLobbyConfig(boolean isPrivate, int impostorCount) {
        if (!client.isConnected() || !isLocalHost()) return;
        client.send(new NetworkMessage(
                NetworkMessage.Type.LOBBY_CONFIG,
                String.valueOf(isPrivate),
                String.valueOf(impostorCount)
        ));
    }

    public void requestStartMatch() {
        if (!client.isConnected() || !isLocalHost()) return;
        client.send(new NetworkMessage(NetworkMessage.Type.START_MATCH));
    }

    public void sendKillRequest(String targetId) {
        if (client.isConnected()) {
            client.send(new NetworkMessage(NetworkMessage.Type.KILL_REQ, targetId));
        }
    }

    public void sendVentToggle() {
        if (client.isConnected()) {
            client.send(new NetworkMessage(NetworkMessage.Type.VENT_REQ, "TOGGLE"));
        }
    }

    public void sendVentCycle(int direction) {
        if (client.isConnected()) {
            client.send(new NetworkMessage(NetworkMessage.Type.VENT_REQ, "CYCLE", String.valueOf(direction)));
        }
    }

    public void sendSabotageRequest(SabotageType type) {
        if (client.isConnected()) {
            client.send(new NetworkMessage(NetworkMessage.Type.SABOTAGE_REQ, type.name()));
        }
    }

    public void sendSabotageRepair() {
        if (client.isConnected()) {
            client.send(new NetworkMessage(NetworkMessage.Type.SABOTAGE_REPAIR));
        }
    }

    public void sendTaskComplete(String taskId) {
        if (client.isConnected()) {
            client.send(new NetworkMessage(NetworkMessage.Type.TASK_COMPLETE, taskId));
        }
    }

    public void sendEmergencyMeetingRequest() {
        if (client.isConnected()) {
            client.send(new NetworkMessage(NetworkMessage.Type.MEETING_REQ, "EMERGENCY", ""));
        }
    }

    public void sendBodyReportRequest(String bodyPlayerId) {
        if (client.isConnected()) {
            client.send(new NetworkMessage(NetworkMessage.Type.MEETING_REQ, "REPORT", bodyPlayerId));
        }
    }

    public void sendChatMessage(Character sender, String text) {
        if (client.isConnected()) {
            client.send(new NetworkMessage(
                    NetworkMessage.Type.CHAT_MSG,
                    localClientId,
                    sender.getName(),
                    sender.getColor().name(),
                    "false",
                    text
            ));
        }
    }

    public void sendVote(String targetId) {
        if (client.isConnected()) {
            client.send(new NetworkMessage(NetworkMessage.Type.VOTE_REQ, targetId));
        }
    }

    public void update(double dt, GameEngine engine) {
        if (!client.isConnected()) {
            if (client.isConnecting()) {
                statusBanner = client.getStatusMessage();
            } else if (hostedServer != null) {
                statusBanner = "Disconnected — Press Reconnect";
            }
            return;
        }

        // Send periodic movement packets (20 Hz)
        moveSyncTimer += dt;
        if (moveSyncTimer >= 0.05 && engine.getState() == GameState.PLAYING) {
            moveSyncTimer = 0.0;
            Character lc = engine.getLocalPlayer().getCharacter();
            client.send(new NetworkMessage(
                    NetworkMessage.Type.MOVE,
                    String.format(Locale.US, "%.1f", lc.getX()),
                    String.format(Locale.US, "%.1f", lc.getY()),
                    String.valueOf(lc.isFacingRight()),
                    lc.getMovementState().name()
            ));
        }

        // Keep-alive ping
        pingTimer += dt;
        if (pingTimer >= 4.0) {
            pingTimer = 0.0;
            client.send(new NetworkMessage(NetworkMessage.Type.PING));
        }

        // Process incoming server messages on game thread
        for (NetworkMessage msg : client.drainInbox()) {
            handleServerMessage(msg, engine);
        }
    }

    private void handleServerMessage(NetworkMessage msg, GameEngine engine) {
        switch (msg.getType()) {
            case WELCOME -> {
                boolean accepted = msg.getBooleanField(0, false);
                String reason = msg.getField(1, "");
                if (!accepted) {
                    statusBanner = "Rejected: " + reason;
                    disconnect();
                    return;
                }
                this.roomCode = msg.getField(2, roomCode);
                this.hostPlayerId = msg.getField(3, "");
                engine.getConfig().setPrivateRoom(msg.getBooleanField(4, true));
                engine.getConfig().setImpostorCount(msg.getIntField(5, 1));
                statusBanner = (hostedServer != null ? "Hosting" : "Connected") + " | Room: " + roomCode;
                engine.setState(GameState.LOBBY);
            }
            case LOBBY_STATE -> {
                this.roomCode = msg.getField(0, roomCode);
                this.hostPlayerId = msg.getField(1, hostPlayerId);
                engine.getConfig().setPrivateRoom(msg.getBooleanField(2, true));
                engine.getConfig().setImpostorCount(msg.getIntField(3, 1));
                engine.applyNetworkLobbyRoster(localClientId, msg.getField(5, ""));
            }
            case START_MATCH -> {
                PlayerRole myRole = parseEnum(PlayerRole.class, msg.getField(0, "CREWMATE"), PlayerRole.CREWMATE);
                engine.startNetworkMatchFromServer(localClientId, myRole, msg.getField(2, ""));
            }
            case STATE_SYNC -> {
                SabotageType sab = parseEnum(SabotageType.class, msg.getField(0, "NONE"), SabotageType.NONE);
                double sabTimer = msg.getDoubleField(1, 0.0);
                float taskPct = (float) msg.getDoubleField(2, 0.0);
                boolean doorsLocked = msg.getBooleanField(3, false);

                engine.getSabotageManager().syncFromNetwork(sab, sabTimer);
                engine.getTaskManager().setNetworkTaskProgress(taskPct);
                if (doorsLocked) {
                    for (Door d : engine.getGameMap().getDoors()) {
                        if (!d.isClosed()) d.lockFor(1.0);
                    }
                }
                engine.applyNetworkStateSync(localClientId, msg.getField(4, ""));
            }
            case KILL_EVENT -> {
                String killerId = msg.getField(0, "");
                String victimId = msg.getField(1, "");
                double vx = msg.getDoubleField(2, 0.0);
                double vy = msg.getDoubleField(3, 0.0);
                engine.applyNetworkKill(killerId, victimId, vx, vy);
            }
            case MEETING_START -> {
                String callerId = msg.getField(0, "");
                String headline = msg.getField(1, "EMERGENCY MEETING");
                engine.applyNetworkMeetingStart(callerId, headline);
            }
            case CHAT_MSG -> {
                String senderName = msg.getField(1, "PLAYER");
                PlayerColor col = parseEnum(PlayerColor.class, msg.getField(2, "WHITE"), PlayerColor.WHITE);
                boolean isSys = msg.getBooleanField(3, false);
                String text = msg.getField(4, "");
                if (isSys) {
                    engine.getMeetingManager().addSystemMessage(text);
                } else {
                    engine.addNetworkChatMessage(senderName, col.getMainColor(), text);
                }
            }
            case VOTE_UPDATE -> {
                String voterId = msg.getField(0, "");
                engine.getVotingManager().forceCastVote(voterId, "HIDDEN", engine.getPlayers());
            }
            case VOTE_RESULT -> {
                String ejectedId = msg.getField(0, "");
                boolean wasTie = msg.getBooleanField(1, false);
                boolean wasSkipped = msg.getBooleanField(2, false);
                String rawPairs = msg.getField(3, "");
                engine.applyNetworkVoteResult(ejectedId, wasTie, wasSkipped, rawPairs);
            }
            case GAME_OVER -> {
                WinConditionManager.Faction winner = parseEnum(
                        WinConditionManager.Faction.class, msg.getField(0, "CREWMATES"), WinConditionManager.Faction.CREWMATES);
                String headline = msg.getField(1, "MATCH OVER");
                String reason = msg.getField(2, "");
                engine.applyNetworkGameOver(new WinConditionManager.MatchOutcome(winner, headline, reason));
            }
            case RETURN_LOBBY -> {
                engine.setState(GameState.LOBBY);
            }
            default -> {}
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String val, E fallback) {
        try {
            return Enum.valueOf(enumType, val.trim().toUpperCase());
        } catch (Exception e) {
            return fallback;
        }
    }

    public boolean isConnected() { return client.isConnected(); }
    public boolean isConnecting() { return client.isConnecting(); }
    public boolean isHosting() { return hostedServer != null && hostedServer.isRunning(); }
    public boolean isLocalHost() { return Objects.equals(localClientId, hostPlayerId); }
    public String getLocalClientId() { return localClientId; }
    public String getRoomCode() { return roomCode; }
    public String getHostPlayerId() { return hostPlayerId; }
    public String getStatusBanner() { return statusBanner; }
}