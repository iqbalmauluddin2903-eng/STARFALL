package starfall.network;

import starfall.entities.*;
import starfall.entities.Character;
import starfall.gameplay.SabotageType;
import starfall.gameplay.VotingManager;
import starfall.gameplay.WinConditionManager;
import starfall.map.GameMap;
import starfall.map.Vent;
import starfall.utils.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;

public class GameServer {

    private static class ServerPlayer {
        final String playerId;
        String name;
        PlayerColor color;
        Cosmetics.Hat hat;
        Cosmetics.Skin skin;
        Cosmetics.Pet pet;

        double x = 1630.0;
        double y = 465.0;
        boolean facingRight = true;
        MovementState movementState = MovementState.IDLE;

        PlayerRole role = PlayerRole.CREWMATE;
        boolean alive = true;
        boolean connected = true;
        boolean bodyReported = false;
        boolean insideVent = false;
        String currentVentId = null;

        int completedTasks = 0;
        int totalTasks = 4;
        double killCooldown = 0.0;
        int meetingsRemaining = 2;

        ClientHandler handler;

        ServerPlayer(String playerId, String name, PlayerColor color, Cosmetics.Hat hat,
                     Cosmetics.Skin skin, Cosmetics.Pet pet, ClientHandler handler) {
            this.playerId = playerId;
            this.name = name;
            this.color = color;
            this.hat = hat;
            this.skin = skin;
            this.pet = pet;
            this.handler = handler;
        }
    }

    private enum ServerPhase {
        LOBBY,
        PLAYING,
        MEETING,
        EJECTION,
        GAME_OVER
    }

    private final int port;
    private final String roomCode;
    private final GameMap serverMap = new GameMap();
    private final WinConditionManager winChecker = new WinConditionManager();
    private final Object stateLock = new Object();
    private final Random rng = new SecureRandom();

    private final Map<String, ServerPlayer> players = new LinkedHashMap<>();
    private final Set<ClientHandler> activeHandlers = ConcurrentHashMap.newKeySet();

    private ServerSocket serverSocket;
    private Thread acceptThread;
    private ScheduledExecutorService tickExecutor;
    private volatile boolean running = false;

    private ServerPhase phase = ServerPhase.LOBBY;
    private String hostPlayerId = null;
    private boolean privateRoom = true;
    private int impostorCount = 1;
    private int maxPlayers = 8;

    // Authoritative gameplay timers
    private SabotageType activeSabotage = SabotageType.NONE;
    private double sabotageCountdown = 0.0;
    private double sabotageCooldown = 0.0;
    private double emergencyCooldown = 0.0;
    private double doorLockTimer = 0.0;

    // Authoritative meeting & voting state
    private boolean discussionPhase = true;
    private double meetingTimer = 0.0;
    private double ejectionTimer = 0.0;
    private double postGameTimer = 0.0;
    private final Map<String, String> votes = new LinkedHashMap<>();

    public GameServer(int port, boolean privateRoom) {
        this.port = port;
        this.privateRoom = privateRoom;
        this.roomCode = generateRoomCode();
    }

    private String generateRoomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(rng.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public void start() throws IOException {
        if (running) return;
        serverSocket = new ServerSocket(port);
        running = true;

        acceptThread = new Thread(this::acceptLoop, "Starfall-ServerAcceptor");
        acceptThread.setDaemon(true);
        acceptThread.start();

        tickExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Starfall-ServerTick");
            t.setDaemon(true);
            return t;
        });
        tickExecutor.scheduleAtFixedRate(this::serverTick, 50, 50, TimeUnit.MILLISECONDS);

        Logger.info("GameServer started on port " + port + " | Room Code: " + roomCode);
    }

    public void stop() {
        if (!running) return;
        running = false;
        if (tickExecutor != null) tickExecutor.shutdownNow();
        for (ClientHandler h : activeHandlers) {
            h.close();
        }
        activeHandlers.clear();
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}
        Logger.info("GameServer stopped.");
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, this);
                activeHandlers.add(handler);
                Thread t = new Thread(handler, "Starfall-ClientHandler-" + clientSocket.getPort());
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                if (running) {
                    Logger.error("Error accepting client socket", e);
                }
            }
        }
    }

    public void handleClientMessage(ClientHandler handler, NetworkMessage msg) {
        if (msg.getType() == NetworkMessage.Type.PING) {
            handler.send(new NetworkMessage(NetworkMessage.Type.PONG));
            return;
        }

        synchronized (stateLock) {
            switch (msg.getType()) {
                case HELLO -> handleHello(handler, msg);
                case CUSTOMIZE -> handleCustomize(handler, msg);
                case LOBBY_CONFIG -> handleLobbyConfig(handler, msg);
                case START_MATCH -> handleStartMatch(handler);
                case MOVE -> handleMove(handler, msg);
                case KILL_REQ -> handleKillRequest(handler, msg);
                case VENT_REQ -> handleVentRequest(handler, msg);
                case SABOTAGE_REQ -> handleSabotageRequest(handler, msg);
                case SABOTAGE_REPAIR -> handleSabotageRepair(handler);
                case TASK_COMPLETE -> handleTaskComplete(handler);
                case MEETING_REQ -> handleMeetingRequest(handler, msg);
                case CHAT_MSG -> handleChatMessage(handler, msg);
                case VOTE_REQ -> handleVoteRequest(handler, msg);
                case DISCONNECT -> {
                    handler.close();
                    onClientDisconnected(handler);
                }
                default -> {}
            }
        }
    }

    private void handleHello(ClientHandler handler, NetworkMessage msg) {
        String pId = msg.getField(0, "p_" + rng.nextInt(100000));
        String requestedCode = msg.getField(1, "").trim().toUpperCase();
        String name = sanitizeName(msg.getField(2, "CREW"), pId);
        PlayerColor color = parseEnum(PlayerColor.class, msg.getField(3, "CYAN"), PlayerColor.CYAN);
        Cosmetics.Hat hat = parseEnum(Cosmetics.Hat.class, msg.getField(4, "NONE"), Cosmetics.Hat.NONE);
        Cosmetics.Skin skin = parseEnum(Cosmetics.Skin.class, msg.getField(5, "DEFAULT"), Cosmetics.Skin.DEFAULT);
        Cosmetics.Pet pet = parseEnum(Cosmetics.Pet.class, msg.getField(6, "NONE"), Cosmetics.Pet.NONE);

        // Validate room code if private or non-empty
        if (privateRoom && !requestedCode.isEmpty() && !requestedCode.equals(roomCode)) {
            handler.send(new NetworkMessage(NetworkMessage.Type.WELCOME, "false", "Invalid Room Code", roomCode, "", "true", "1"));
            return;
        }
        if (privateRoom && requestedCode.isEmpty() && !players.isEmpty()) {
            handler.send(new NetworkMessage(NetworkMessage.Type.WELCOME, "false", "Room Code Required", roomCode, "", "true", "1"));
            return;
        }

        // Reconnection handling for existing player ID
        ServerPlayer existing = players.get(pId);
        if (existing != null) {
            existing.connected = true;
            existing.handler = handler;
            handler.setPlayerId(pId);
            handler.send(new NetworkMessage(NetworkMessage.Type.WELCOME, "true", "Reconnected",
                    roomCode, hostPlayerId, String.valueOf(privateRoom), String.valueOf(impostorCount)));
            broadcastSystemChat(existing.name + " reconnected to the session.");
            broadcastLobbyState();
            if (phase != ServerPhase.LOBBY) {
                sendMatchStartToPlayer(existing);
            }
            return;
        }

        if (phase != ServerPhase.LOBBY) {
            handler.send(new NetworkMessage(NetworkMessage.Type.WELCOME, "false", "Match already in progress", roomCode, "", "true", "1"));
            return;
        }
        if (players.size() >= maxPlayers) {
            handler.send(new NetworkMessage(NetworkMessage.Type.WELCOME, "false", "Room is full", roomCode, "", "true", "1"));
            return;
        }

        int slotIdx = players.size();
        double angle = (slotIdx * Math.PI * 2.0) / maxPlayers;
        ServerPlayer sp = new ServerPlayer(pId, name, color, hat, skin, pet, handler);
        sp.x = 1630.0 + Math.cos(angle) * 145.0;
        sp.y = 360.0 + Math.sin(angle) * 125.0;

        players.put(pId, sp);
        handler.setPlayerId(pId);
        if (hostPlayerId == null) {
            hostPlayerId = pId;
        }

        handler.send(new NetworkMessage(NetworkMessage.Type.WELCOME, "true", "Joined",
                roomCode, hostPlayerId, String.valueOf(privateRoom), String.valueOf(impostorCount)));
        broadcastSystemChat(sp.name + " joined the lobby.");
        broadcastLobbyState();
    }

    private void handleCustomize(ClientHandler handler, NetworkMessage msg) {
        ServerPlayer sp = players.get(handler.getPlayerId());
        if (sp == null || phase != ServerPhase.LOBBY) return;

        sp.name = sanitizeName(msg.getField(0, sp.name), sp.playerId);
        sp.color = parseEnum(PlayerColor.class, msg.getField(1, sp.color.name()), sp.color);
        sp.hat = parseEnum(Cosmetics.Hat.class, msg.getField(2, sp.hat.name()), sp.hat);
        sp.skin = parseEnum(Cosmetics.Skin.class, msg.getField(3, sp.skin.name()), sp.skin);
        sp.pet = parseEnum(Cosmetics.Pet.class, msg.getField(4, sp.pet.name()), sp.pet);
        broadcastLobbyState();
    }

    private void handleLobbyConfig(ClientHandler handler, NetworkMessage msg) {
        if (!Objects.equals(handler.getPlayerId(), hostPlayerId) || phase != ServerPhase.LOBBY) return;
        this.privateRoom = msg.getBooleanField(0, privateRoom);
        this.impostorCount = Math.max(1, Math.min(3, msg.getIntField(1, impostorCount)));
        broadcastLobbyState();
    }

    private void handleStartMatch(ClientHandler handler) {
        if (!Objects.equals(handler.getPlayerId(), hostPlayerId) || phase != ServerPhase.LOBBY) return;
        if (players.size() < 2) {
            broadcastSystemChat("Need at least 2 players in lobby to start match!");
            return;
        }

        phase = ServerPhase.PLAYING;
        activeSabotage = SabotageType.NONE;
        sabotageCountdown = 0.0;
        sabotageCooldown = 0.0;
        emergencyCooldown = 12.0;
        doorLockTimer = 0.0;
        votes.clear();

        List<ServerPlayer> roster = new ArrayList<>(players.values());
        Collections.shuffle(roster, rng);

        int actualImpostors = Math.min(impostorCount, Math.max(1, roster.size() / 3));
        if (roster.size() == 2) actualImpostors = 1;

        for (int i = 0; i < roster.size(); i++) {
            ServerPlayer sp = roster.get(i);
            sp.role = (i < actualImpostors) ? PlayerRole.IMPOSTOR : PlayerRole.CREWMATE;
            sp.alive = true;
            sp.bodyReported = false;
            sp.insideVent = false;
            sp.currentVentId = null;
            sp.completedTasks = 0;
            sp.totalTasks = 4;
            sp.killCooldown = 10.0;
            sp.meetingsRemaining = 2;

            double angle = (i * Math.PI * 2.0) / roster.size();
            sp.x = 1630.0 + Math.cos(angle) * 145.0;
            sp.y = 360.0 + Math.sin(angle) * 125.0;
            sp.movementState = MovementState.IDLE;
        }

        for (ServerPlayer sp : players.values()) {
            sendMatchStartToPlayer(sp);
        }
        Logger.info("Authoritative match started with " + roster.size() + " players (" + actualImpostors + " Impostor).");
    }

    private void sendMatchStartToPlayer(ServerPlayer recipient) {
        StringBuilder sb = new StringBuilder();
        for (ServerPlayer sp : players.values()) {
            if (!sb.isEmpty()) sb.append(';');
            // Only reveal Impostor roles to fellow Impostors; Crewmates see CREWMATE until revealed
            PlayerRole visibleRole = (recipient.role == PlayerRole.IMPOSTOR || sp == recipient)
                    ? sp.role
                    : PlayerRole.CREWMATE;
            sb.append(sp.playerId).append(',')
              .append(sp.name).append(',')
              .append(sp.color.name()).append(',')
              .append(sp.hat.name()).append(',')
              .append(sp.skin.name()).append(',')
              .append(sp.pet.name()).append(',')
              .append(visibleRole.name()).append(',')
              .append(String.format(Locale.US, "%.1f", sp.x)).append(',')
              .append(String.format(Locale.US, "%.1f", sp.y));
        }
        if (recipient.handler != null) {
            recipient.handler.send(new NetworkMessage(
                    NetworkMessage.Type.START_MATCH,
                    recipient.role.name(),
                    String.valueOf(players.size()),
                    sb.toString()
            ));
        }
    }

    private void handleMove(ClientHandler handler, NetworkMessage msg) {
        if (phase != ServerPhase.PLAYING) return;
        ServerPlayer sp = players.get(handler.getPlayerId());
        if (sp == null || sp.insideVent) return;

        double nx = msg.getDoubleField(0, sp.x);
        double ny = msg.getDoubleField(1, sp.y);
        boolean facingRight = msg.getBooleanField(2, sp.facingRight);
        MovementState mState = parseEnum(MovementState.class, msg.getField(3, "IDLE"), MovementState.IDLE);

        // Clamp within map bounds and reject extreme teleport spikes
        if (nx >= 50 && nx <= GameMap.WORLD_WIDTH - 50 && ny >= 50 && ny <= GameMap.WORLD_HEIGHT - 50) {
            if (Math.hypot(nx - sp.x, ny - sp.y) < 350.0) {
                sp.x = nx;
                sp.y = ny;
            }
        }
        sp.facingRight = facingRight;
        sp.movementState = sp.alive ? mState : MovementState.GHOST_FLOATING;
    }

    private void handleKillRequest(ClientHandler handler, NetworkMessage msg) {
        if (phase != ServerPhase.PLAYING) return;
        ServerPlayer killer = players.get(handler.getPlayerId());
        ServerPlayer victim = players.get(msg.getField(0, ""));
        if (killer == null || victim == null) return;
        if (!killer.alive || killer.role != PlayerRole.IMPOSTOR || killer.insideVent || killer.killCooldown > 0.0) return;
        if (!victim.alive || victim.role == PlayerRole.IMPOSTOR) return;

        if (Math.hypot(killer.x - victim.x, killer.y - victim.y) <= 125.0) {
            killer.x = victim.x;
            killer.y = victim.y;
            killer.killCooldown = 20.0;
            victim.alive = false;
            victim.movementState = MovementState.DEAD;

            broadcast(new NetworkMessage(
                    NetworkMessage.Type.KILL_EVENT,
                    killer.playerId,
                    victim.playerId,
                    String.format(Locale.US, "%.1f", victim.x),
                    String.format(Locale.US, "%.1f", victim.y)
            ));
            evaluateServerWinConditions();
        }
    }

    private void handleVentRequest(ClientHandler handler, NetworkMessage msg) {
        if (phase != ServerPhase.PLAYING) return;
        ServerPlayer imp = players.get(handler.getPlayerId());
        if (imp == null || !imp.alive || imp.role != PlayerRole.IMPOSTOR) return;

        String action = msg.getField(0, "TOGGLE");
        if ("TOGGLE".equals(action)) {
            if (imp.insideVent) {
                imp.insideVent = false;
                imp.currentVentId = null;
                imp.movementState = MovementState.IDLE;
            } else {
                Vent nearest = serverMap.findNearestVent(imp.x, imp.y, 100.0);
                if (nearest != null) {
                    imp.x = nearest.getX();
                    imp.y = nearest.getY();
                    imp.insideVent = true;
                    imp.currentVentId = nearest.getVentId();
                    imp.movementState = MovementState.VENTING;
                }
            }
        } else if ("CYCLE".equals(action) && imp.insideVent && imp.currentVentId != null) {
            int dir = msg.getIntField(1, 1);
            for (Vent v : serverMap.getVents()) {
                if (v.getVentId().equals(imp.currentVentId) && !v.getConnectedVentIds().isEmpty()) {
                    List<String> links = v.getConnectedVentIds();
                    String nextId = links.get(Math.floorMod(dir, links.size()));
                    for (Vent target : serverMap.getVents()) {
                        if (target.getVentId().equals(nextId)) {
                            imp.x = target.getX();
                            imp.y = target.getY();
                            imp.currentVentId = target.getVentId();
                            break;
                        }
                    }
                    break;
                }
            }
        }
    }

    private void handleSabotageRequest(ClientHandler handler, NetworkMessage msg) {
        if (phase != ServerPhase.PLAYING) return;
        ServerPlayer imp = players.get(handler.getPlayerId());
        if (imp == null || !imp.alive || imp.role != PlayerRole.IMPOSTOR) return;
        if (activeSabotage != SabotageType.NONE || sabotageCooldown > 0.0) return;

        SabotageType requested = parseEnum(SabotageType.class, msg.getField(0, "NONE"), SabotageType.NONE);
        if (requested == SabotageType.NONE) return;

        if (requested == SabotageType.DOOR_LOCKDOWN) {
            doorLockTimer = 10.0;
            sabotageCooldown = 18.0;
        } else {
            activeSabotage = requested;
            sabotageCountdown = requested.getDurationSeconds();
            sabotageCooldown = 25.0;
        }
    }

    private void handleSabotageRepair(ClientHandler handler) {
        if (phase != ServerPhase.PLAYING) return;
        ServerPlayer sp = players.get(handler.getPlayerId());
        if (sp == null || !sp.alive) return;
        activeSabotage = SabotageType.NONE;
        sabotageCountdown = 0.0;
    }

    private void handleTaskComplete(ClientHandler handler) {
        if (phase != ServerPhase.PLAYING) return;
        ServerPlayer sp = players.get(handler.getPlayerId());
        if (sp == null || sp.role == PlayerRole.IMPOSTOR) return;

        sp.completedTasks = Math.min(sp.totalTasks, sp.completedTasks + 1);
        evaluateServerWinConditions();
    }

    private void handleMeetingRequest(ClientHandler handler, NetworkMessage msg) {
        if (phase != ServerPhase.PLAYING) return;
        ServerPlayer caller = players.get(handler.getPlayerId());
        if (caller == null || !caller.alive) return;

        String kind = msg.getField(0, "EMERGENCY");
        String headline;

        if ("REPORT".equals(kind)) {
            ServerPlayer body = players.get(msg.getField(1, ""));
            if (body == null || body.alive || body.bodyReported) return;
            body.bodyReported = true;
            activeSabotage = SabotageType.NONE;
            sabotageCountdown = 0.0;
            headline = "DEAD BODY REPORTED — " + body.name + " FOUND BY " + caller.name;
        } else {
            if (activeSabotage != SabotageType.NONE || emergencyCooldown > 0.0 || caller.meetingsRemaining <= 0) return;
            caller.meetingsRemaining--;
            emergencyCooldown = 12.0;
            headline = "EMERGENCY MEETING CALLED BY " + caller.name;
        }

        // Gather living players at Cafeteria and convert dead bodies to ghosts
        int idx = 0;
        int totalLiving = 0;
        for (ServerPlayer sp : players.values()) {
            if (sp.alive) totalLiving++;
        }
        for (ServerPlayer sp : players.values()) {
            sp.insideVent = false;
            sp.currentVentId = null;
            if (!sp.alive) {
                sp.bodyReported = true;
                sp.role = PlayerRole.GHOST;
                sp.movementState = MovementState.GHOST_FLOATING;
            } else {
                double angle = (idx * Math.PI * 2.0) / Math.max(1, totalLiving);
                sp.x = 1630.0 + Math.cos(angle) * 145.0;
                sp.y = 360.0 + Math.sin(angle) * 125.0;
                sp.movementState = MovementState.IDLE;
                idx++;
            }
        }

        votes.clear();
        phase = ServerPhase.MEETING;
        discussionPhase = true;
        meetingTimer = 6.0;

        broadcast(new NetworkMessage(NetworkMessage.Type.MEETING_START, caller.playerId, headline, "6.0", "35.0"));
    }

    private void handleChatMessage(ClientHandler handler, NetworkMessage msg) {
        ServerPlayer sender = players.get(handler.getPlayerId());
        if (sender == null) return;
        String text = NetworkMessage.sanitize(msg.getField(4, ""));
        if (text.isBlank()) return;
        if (text.length() > 80) text = text.substring(0, 80);

        String displayName = sender.alive ? sender.name : "[GHOST] " + sender.name;
        broadcast(new NetworkMessage(
                NetworkMessage.Type.CHAT_MSG,
                sender.playerId,
                displayName,
                sender.color.name(),
                "false",
                text
        ));
    }

    private void handleVoteRequest(ClientHandler handler, NetworkMessage msg) {
        if (phase != ServerPhase.MEETING || discussionPhase) return;
        ServerPlayer voter = players.get(handler.getPlayerId());
        if (voter == null || !voter.alive || votes.containsKey(voter.playerId)) return;

        String targetId = msg.getField(0, VotingManager.SKIP_VOTE);
        if (!VotingManager.SKIP_VOTE.equals(targetId)) {
            ServerPlayer target = players.get(targetId);
            if (target == null || !target.alive) return;
        }

        votes.put(voter.playerId, targetId);
        broadcast(new NetworkMessage(NetworkMessage.Type.VOTE_UPDATE, voter.playerId));
    }

    private void serverTick() {
        synchronized (stateLock) {
            double dt = 0.05;

            if (phase == ServerPhase.LOBBY) {
                broadcastLobbyState();
                return;
            }

            if (phase == ServerPhase.PLAYING) {
                if (emergencyCooldown > 0) emergencyCooldown = Math.max(0.0, emergencyCooldown - dt);
                if (sabotageCooldown > 0) sabotageCooldown = Math.max(0.0, sabotageCooldown - dt);
                if (doorLockTimer > 0) doorLockTimer = Math.max(0.0, doorLockTimer - dt);

                for (ServerPlayer sp : players.values()) {
                    if (sp.killCooldown > 0 && !sp.insideVent) {
                        sp.killCooldown = Math.max(0.0, sp.killCooldown - dt);
                    }
                }

                if (activeSabotage != SabotageType.NONE && activeSabotage.getDurationSeconds() > 0) {
                    sabotageCountdown -= dt;
                    if (sabotageCountdown <= 0.0) {
                        sabotageCountdown = 0.0;
                        if (activeSabotage.isLethalOnTimeout()) {
                            triggerServerGameOver(WinConditionManager.Faction.IMPOSTORS,
                                    "IMPOSTORS VICTORY", "Critical ship sabotage was not repaired in time!");
                            return;
                        }
                        activeSabotage = SabotageType.NONE;
                    }
                }

                broadcastStateSync();
                evaluateServerWinConditions();
            } else if (phase == ServerPhase.MEETING) {
                meetingTimer -= dt;
                if (discussionPhase) {
                    if (meetingTimer <= 0.0) {
                        discussionPhase = false;
                        meetingTimer = 35.0;
                    }
                } else {
                    if (allConnectedLivingVoted() || meetingTimer <= 0.0) {
                        finalizeServerVoting();
                    }
                }
            } else if (phase == ServerPhase.EJECTION) {
                ejectionTimer -= dt;
                if (ejectionTimer <= 0.0) {
                    if (!evaluateServerWinConditions()) {
                        phase = ServerPhase.PLAYING;
                        broadcastStateSync();
                    }
                }
            } else if (phase == ServerPhase.GAME_OVER) {
                postGameTimer -= dt;
                if (postGameTimer <= 0.0) {
                    phase = ServerPhase.LOBBY;
                    for (ServerPlayer sp : players.values()) {
                        sp.alive = true;
                        sp.role = PlayerRole.CREWMATE;
                        sp.movementState = MovementState.IDLE;
                    }
                    broadcast(new NetworkMessage(NetworkMessage.Type.RETURN_LOBBY, roomCode));
                    broadcastLobbyState();
                }
            }
        }
    }

    private boolean allConnectedLivingVoted() {
        for (ServerPlayer sp : players.values()) {
            if (sp.alive && sp.connected && !votes.containsKey(sp.playerId)) {
                return false;
            }
        }
        return true;
    }

    private void finalizeServerVoting() {
        Map<String, Integer> tally = new LinkedHashMap<>();
        tally.put(VotingManager.SKIP_VOTE, 0);
        for (ServerPlayer sp : players.values()) {
            if (sp.alive) tally.put(sp.playerId, 0);
        }
        for (Map.Entry<String, String> e : votes.entrySet()) {
            ServerPlayer voter = players.get(e.getKey());
            if (voter != null && voter.alive && tally.containsKey(e.getValue())) {
                tally.put(e.getValue(), tally.get(e.getValue()) + 1);
            }
        }

        int max = 0;
        for (int v : tally.values()) if (v > max) max = v;

        List<String> top = new ArrayList<>();
        if (max > 0) {
            for (Map.Entry<String, Integer> e : tally.entrySet()) {
                if (e.getValue() == max) top.add(e.getKey());
            }
        }

        String ejectedId = "";
        boolean wasTie = false;
        boolean wasSkipped = false;

        if (max == 0) {
            wasSkipped = true;
        } else if (top.size() > 1) {
            wasTie = true;
        } else if (VotingManager.SKIP_VOTE.equals(top.get(0))) {
            wasSkipped = true;
        } else {
            ejectedId = top.get(0);
            ServerPlayer ejected = players.get(ejectedId);
            if (ejected != null) {
                ejected.alive = false;
                ejected.movementState = MovementState.GHOST_FLOATING;
            }
        }

        StringBuilder votePairs = new StringBuilder();
        for (Map.Entry<String, String> e : votes.entrySet()) {
            if (!votePairs.isEmpty()) votePairs.append(';');
            votePairs.append(e.getKey()).append('=').append(e.getValue());
        }

        phase = ServerPhase.EJECTION;
        ejectionTimer = 4.6;
        broadcast(new NetworkMessage(
                NetworkMessage.Type.VOTE_RESULT,
                ejectedId,
                String.valueOf(wasTie),
                String.valueOf(wasSkipped),
                votePairs.toString()
        ));
    }

    private boolean evaluateServerWinConditions() {
        if (players.isEmpty()) return false;

        List<Player> proxyList = new ArrayList<>();
        int totalTasks = 0;
        int doneTasks = 0;

        for (ServerPlayer sp : players.values()) {
            Character c = (sp.role == PlayerRole.IMPOSTOR)
                    ? new Impostor(sp.playerId, sp.name, sp.x, sp.y, sp.color)
                    : new Crewmate(sp.playerId, sp.name, sp.x, sp.y, sp.color);
            if (!sp.alive) c.kill();
            c.setConnected(sp.connected);
            proxyList.add(new Player(sp.playerId, c, false, false));

            if (sp.role != PlayerRole.IMPOSTOR) {
                totalTasks += sp.totalTasks;
                doneTasks += sp.completedTasks;
            }
        }

        if (totalTasks > 0 && doneTasks >= totalTasks) {
            triggerServerGameOver(WinConditionManager.Faction.CREWMATES,
                    "CREWMATES VICTORY", "All crewmates completed their ship tasks!");
            return true;
        }

        int aliveCrew = 0;
        int aliveImp = 0;
        int totalImp = 0;
        for (ServerPlayer sp : players.values()) {
            if (!sp.connected) continue;
            if (sp.role == PlayerRole.IMPOSTOR) {
                totalImp++;
                if (sp.alive) aliveImp++;
            } else if (sp.alive) {
                aliveCrew++;
            }
        }

        if (totalImp == 0 || aliveImp == 0) {
            triggerServerGameOver(WinConditionManager.Faction.CREWMATES,
                    "CREWMATES VICTORY", "All Impostors have been eliminated!");
            return true;
        }
        if (aliveImp >= aliveCrew) {
            triggerServerGameOver(WinConditionManager.Faction.IMPOSTORS,
                    "IMPOSTORS VICTORY", "Impostors reached numerical parity (" + aliveImp + " vs " + aliveCrew + ")!");
            return true;
        }
        return false;
    }

    private void triggerServerGameOver(WinConditionManager.Faction winner, String headline, String reason) {
        if (phase == ServerPhase.GAME_OVER) return;
        phase = ServerPhase.GAME_OVER;
        postGameTimer = 6.0;
        broadcast(new NetworkMessage(NetworkMessage.Type.GAME_OVER, winner.name(), headline, reason));
    }

    private void broadcastLobbyState() {
        StringBuilder sb = new StringBuilder();
        for (ServerPlayer sp : players.values()) {
            if (!sb.isEmpty()) sb.append(';');
            sb.append(sp.playerId).append(',')
              .append(sp.name).append(',')
              .append(sp.color.name()).append(',')
              .append(sp.hat.name()).append(',')
              .append(sp.skin.name()).append(',')
              .append(sp.pet.name()).append(',')
              .append(sp.connected);
        }
        broadcast(new NetworkMessage(
                NetworkMessage.Type.LOBBY_STATE,
                roomCode,
                hostPlayerId != null ? hostPlayerId : "",
                String.valueOf(privateRoom),
                String.valueOf(impostorCount),
                String.valueOf(players.size()),
                sb.toString()
        ));
    }

    private void broadcastStateSync() {
        int totalTasks = 0;
        int doneTasks = 0;
        StringBuilder sb = new StringBuilder();

        for (ServerPlayer sp : players.values()) {
            if (sp.role != PlayerRole.IMPOSTOR) {
                totalTasks += sp.totalTasks;
                doneTasks += sp.completedTasks;
            }
            if (!sb.isEmpty()) sb.append(';');
            sb.append(sp.playerId).append(',')
              .append(String.format(Locale.US, "%.1f", sp.x)).append(',')
              .append(String.format(Locale.US, "%.1f", sp.y)).append(',')
              .append(sp.facingRight).append(',')
              .append(sp.movementState.name()).append(',')
              .append(sp.alive).append(',')
              .append(sp.connected).append(',')
              .append(sp.insideVent);
        }

        float taskPct = (totalTasks > 0) ? (doneTasks / (float) totalTasks) : 1.0f;
        broadcast(new NetworkMessage(
                NetworkMessage.Type.STATE_SYNC,
                activeSabotage.name(),
                String.format(Locale.US, "%.2f", sabotageCountdown),
                String.format(Locale.US, "%.3f", taskPct),
                String.valueOf(doorLockTimer > 0),
                sb.toString()
        ));
    }

    private void broadcastSystemChat(String text) {
        broadcast(new NetworkMessage(NetworkMessage.Type.CHAT_MSG, "SYSTEM", "SYSTEM", "YELLOW", "true", text));
    }

    private void broadcast(NetworkMessage msg) {
        for (ClientHandler h : activeHandlers) {
            h.send(msg);
        }
    }

    public void onClientDisconnected(ClientHandler handler) {
        activeHandlers.remove(handler);
        String pId = handler.getPlayerId();
        if (pId == null) return;

        synchronized (stateLock) {
            ServerPlayer sp = players.get(pId);
            if (sp == null) return;

            if (phase == ServerPhase.LOBBY) {
                players.remove(pId);
                if (Objects.equals(hostPlayerId, pId)) {
                    hostPlayerId = players.isEmpty() ? null : players.keySet().iterator().next();
                }
                broadcastSystemChat(sp.name + " left the lobby.");
                broadcastLobbyState();
            } else {
                sp.connected = false;
                broadcastSystemChat(sp.name + " disconnected (waiting for reconnect)...");
                evaluateServerWinConditions();
            }
        }
    }

    private String sanitizeName(String raw, String fallbackId) {
        String clean = NetworkMessage.sanitize(raw).trim();
        if (clean.isEmpty()) clean = "PLAYER_" + fallbackId.substring(Math.max(0, fallbackId.length() - 3));
        return clean.length() > 14 ? clean.substring(0, 14) : clean;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String val, E fallback) {
        try {
            return Enum.valueOf(enumType, val.trim().toUpperCase());
        } catch (Exception e) {
            return fallback;
        }
    }

    public String getRoomCode() { return roomCode; }
    public int getPort() { return port; }
    public boolean isRunning() { return running; }

    // Dedicated Server Standalone Entry Point
    public static void main(String[] args) {
        int port = 25565;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }
        GameServer dedicated = new GameServer(port, false);
        try {
            dedicated.start();
            System.out.println("=== STARFALL DEDICATED SERVER RUNNING ===");
            System.out.println("Port: " + port + " | Room Code: " + dedicated.getRoomCode());
            System.out.println("Press Ctrl+C to stop server.");
            Thread.currentThread().join();
        } catch (Exception e) {
            Logger.error("Dedicated server failed", e);
        }
    }
}