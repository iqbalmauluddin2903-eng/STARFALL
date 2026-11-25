package starfall.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.*;
import starfall.ai.AIController;
import starfall.ai.AINavigation;
import starfall.ai.CrewmateAI;
import starfall.ai.ImpostorAI;
import starfall.audio.AudioManager;
import starfall.entities.*;
import starfall.entities.Character;
import starfall.gameplay.*;
import starfall.graphics.Camera;
import starfall.graphics.ParticleSystem;
import starfall.graphics.Renderer;
import starfall.map.GameMap;
import starfall.map.InteractiveObject;
import starfall.map.RoomId;
import starfall.network.NetworkManager;
import starfall.ui.*;
import starfall.utils.Logger;

public class GameEngine {

    private final GameConfig config;
    private final InputManager inputManager;
    private final GameMap gameMap;
    private final AINavigation aiNavigation;
    private final Camera camera;
    private final Renderer renderer;
    private final GamePanel gamePanel;
    private final GameWindow gameWindow;
    private final GameLoop gameLoop;
    private final AudioManager audioManager;
    private final ParticleSystem particleSystem = new ParticleSystem();

    private final TaskManager taskManager = new TaskManager();
    private final KillManager killManager = new KillManager();
    private final VentManager ventManager = new VentManager();
    private final SabotageManager sabotageManager = new SabotageManager();
    private final MeetingManager meetingManager = new MeetingManager();
    private final VotingManager votingManager = new VotingManager();
    private final WinConditionManager winConditionManager = new WinConditionManager();
    private final NetworkManager networkManager = new NetworkManager();

    private final MainMenu mainMenu = new MainMenu();
    private final SettingsScreen settingsScreen = new SettingsScreen();
    private final LobbyScreen lobbyScreen = new LobbyScreen();
    private final MeetingScreen meetingScreen = new MeetingScreen();
    private final EjectionScreen ejectionScreen = new EjectionScreen();
    private final GameOverScreen gameOverScreen = new GameOverScreen();
    private final SurveillanceOverlay surveillanceOverlay = new SurveillanceOverlay();

    private final List<Player> players = new ArrayList<>();
    private final List<AIController> aiControllers = new ArrayList<>();
    private final Random rng = new Random();
    private Player localPlayer;

    private volatile GameState state = GameState.MAIN_MENU;
    private WinConditionManager.MatchOutcome matchOutcome = null;

    private float screenFadeAlpha = 1.0f;
    private double footstepTimer = 0.0;
    private double sabotageSparkTimer = 0.0;
    private RoomId lastAnnouncedRoom = RoomId.CAFETERIA;
    private float roomBannerAlpha = 1.0f;
    private InteractiveObject nearbyInteractable = null;
    private Character killTarget = null;
    private Character reportableBody = null;
    private boolean nearVent = false;

    public GameEngine() {
        this.config = new GameConfig();
        this.inputManager = new InputManager();
        this.inputManager.setAllowArrowKeys(config.getControlScheme() == GameConfig.ControlScheme.WASD_AND_ARROWS);
        this.gameMap = new GameMap();
        this.aiNavigation = new AINavigation();
        this.camera = new Camera(config.getWindowWidth(), config.getWindowHeight());
        this.renderer = new Renderer(config.getWindowWidth(), config.getWindowHeight());
        this.gamePanel = new GamePanel(this, config.getWindowWidth(), config.getWindowHeight());
        this.gameWindow = new GameWindow(config, gamePanel, this::shutdown);
        this.gameLoop = new GameLoop(config.getTargetFps(), this::update, gamePanel::repaint);
        this.audioManager = new AudioManager(config);

        gamePanel.addKeyListener(inputManager);
        gamePanel.addMouseListener(inputManager);
        gamePanel.addMouseMotionListener(inputManager);

        Crewmate initialChar = new Crewmate("player_local", "CAPTAIN", 1630, 465, PlayerColor.CYAN);
        initialChar.setHat(Cosmetics.Hat.CROWN);
        initialChar.setPet(Cosmetics.Pet.MINI_CREW);
        this.localPlayer = new Player("player_local", initialChar, true, false);
        players.add(localPlayer);
    }

    public void transitionToState(GameState targetState) {
        this.state = targetState;
        this.screenFadeAlpha = 1.0f;
        surveillanceOverlay.close();
        boolean menuMusic = (targetState == GameState.MAIN_MENU
                || targetState == GameState.SETTINGS
                || targetState == GameState.HOW_TO_PLAY
                || targetState == GameState.CREDITS
                || targetState == GameState.LOBBY);
        audioManager.setMenuMusicMode(menuMusic);
    }

    public void applyResolutionChange(int width, int height) {
        gameWindow.applyResolution(width, height);
        camera.setViewportSize(width, height);
    }

    public void toggleFullscreenMode() {
        gameWindow.toggleFullscreen();
        camera.setViewportSize(gamePanel.getWidth(), gamePanel.getHeight());
    }

    public void startOfflineMatchFromConfig() {
        int totalPlayers = config.getOfflinePlayerCount();
        int impCount = Math.min(config.getImpostorCount(), Math.max(1, totalPlayers / 3));
        boolean localIsImp = rng.nextInt(totalPlayers) < impCount;
        startNewMatch(localIsImp, totalPlayers, impCount);
    }

    public void startNewMatch(boolean playAsImpostor) {
        startNewMatch(playAsImpostor, config.getOfflinePlayerCount(), config.getImpostorCount());
    }

    public void startNewMatch(boolean playAsImpostor, int totalPlayers, int desiredImpostors) {
        if (networkManager.isConnected()) {
            networkManager.disconnect();
        }
        players.clear();
        aiControllers.clear();
        particleSystem.clear();
        sabotageManager.reset();
        meetingManager.reset(config);
        votingManager.reset();
        meetingScreen.resetInput();
        taskManager.closeActiveTask();
        surveillanceOverlay.close();
        inputManager.clearTypedCharacters();

        matchOutcome = null;
        transitionToState(GameState.PLAYING);

        String prevName = (localPlayer != null) ? localPlayer.getCharacter().getName() : "CAPTAIN";
        PlayerColor prevCol = (localPlayer != null) ? localPlayer.getCharacter().getColor() : PlayerColor.CYAN;
        Cosmetics.Hat prevHat = (localPlayer != null) ? localPlayer.getCharacter().getHat() : Cosmetics.Hat.CROWN;
        Cosmetics.Skin prevSkin = (localPlayer != null) ? localPlayer.getCharacter().getSkin() : Cosmetics.Skin.DEFAULT;
        Cosmetics.Pet prevPet = (localPlayer != null) ? localPlayer.getCharacter().getPet() : Cosmetics.Pet.MINI_CREW;

        Character localChar = playAsImpostor
                ? new Impostor("player_local", prevName, 1630, 465, prevCol)
                : new Crewmate("player_local", prevName, 1630, 465, prevCol);
        localChar.setHat(prevHat);
        localChar.setSkin(prevSkin);
        localChar.setPet(prevPet);

        this.localPlayer = new Player("player_local", localChar, true, false);
        players.add(localPlayer);

        int botCount = Math.max(3, Math.min(9, totalPlayers - 1));
        int remainingImpostors = playAsImpostor ? Math.max(0, desiredImpostors - 1) : Math.max(1, desiredImpostors);

        List<Integer> botIndices = new ArrayList<>();
        for (int i = 0; i < botCount; i++) botIndices.add(i);
        Collections.shuffle(botIndices, rng);
        Set<Integer> impSet = new HashSet<>(botIndices.subList(0, Math.min(remainingImpostors, botCount)));

        String[] botNames = {"NOVA", "ORION", "VEGA", "ATLAS", "LYRA", "DRACO", "SIRIUS", "ALTAIR", "RIGEL"};
        PlayerColor[] botColors = {
            PlayerColor.BLUE, PlayerColor.LIME, PlayerColor.PINK,
            PlayerColor.YELLOW, PlayerColor.ORANGE, PlayerColor.PURPLE,
            PlayerColor.WHITE, PlayerColor.BROWN, PlayerColor.GREEN
        };
        Cosmetics.Hat[] botHats = {
            Cosmetics.Hat.ASTRONAUT, Cosmetics.Hat.LEAF, Cosmetics.Hat.BEANIE,
            Cosmetics.Hat.VIKING, Cosmetics.Hat.COWBOY, Cosmetics.Hat.NONE,
            Cosmetics.Hat.CROWN, Cosmetics.Hat.BEANIE, Cosmetics.Hat.LEAF
        };
        Cosmetics.Skin[] botSkins = {
            Cosmetics.Skin.CAPTAIN, Cosmetics.Skin.LAB_COAT, Cosmetics.Skin.LAB_COAT,
            Cosmetics.Skin.MECHANIC, Cosmetics.Skin.SECURITY, Cosmetics.Skin.DEFAULT,
            Cosmetics.Skin.CAPTAIN, Cosmetics.Skin.MECHANIC, Cosmetics.Skin.DEFAULT
        };

        int aiCrewmateCount = 0;
        for (int i = 0; i < botCount; i++) {
            double angle = (i * Math.PI * 2.0) / botCount;
            double sx = 1630 + Math.cos(angle) * 145;
            double sy = 360 + Math.sin(angle) * 125;
            boolean isImp = impSet.contains(i);
            if (!isImp) aiCrewmateCount++;

            addMatchBot("bot_" + (i + 1), botNames[i], sx, sy, botColors[i], botHats[i], botSkins[i], isImp);
        }

        taskManager.assignTasksForMatch(gameMap, 5, aiCrewmateCount);
        camera.snapTo(localChar.getX(), localChar.getY());
        Logger.info("Offline match started. Local Role: " + localChar.getRole().getDisplayName()
                + " | Total Players: " + players.size());
    }

    private void addMatchBot(String id, String name, double x, double y, PlayerColor color,
                             Cosmetics.Hat hat, Cosmetics.Skin skin, boolean isImpostor) {
        Character c = isImpostor
                ? new Impostor(id, name, x, y, color)
                : new Crewmate(id, name, x, y, color);
        c.setHat(hat);
        c.setSkin(skin);
        Player botPlayer = new Player(id, c, false, true);
        players.add(botPlayer);

        AIController controller = isImpostor
                ? new ImpostorAI(botPlayer, aiNavigation)
                : new CrewmateAI(botPlayer, aiNavigation);
        aiControllers.add(controller);
    }

    public void start() {
        Logger.info("Starting " + GameConfig.GAME_TITLE + " (" + GameConfig.VERSION + ")");
        gameWindow.showWindow();
        gamePanel.requestFocusInWindow();
        audioManager.startMusicLoop();
        gameLoop.start();
    }

    private void update(double dt) {
        if (screenFadeAlpha > 0.0f) {
            screenFadeAlpha = Math.max(0.0f, screenFadeAlpha - (float) (dt * 2.4));
        }

        networkManager.update(dt, this);

        if (inputManager.consumeKeyPress(KeyEvent.VK_F3)) {
            config.toggleDebugHud();
        }
        if (inputManager.consumeKeyPress(KeyEvent.VK_F11)) {
            toggleFullscreenMode();
        }

        // MAIN MENU, HOW TO PLAY, CREDITS
        if (state == GameState.MAIN_MENU || state == GameState.HOW_TO_PLAY || state == GameState.CREDITS) {
            if (inputManager.consumeKeyPress(KeyEvent.VK_ESCAPE) && state != GameState.MAIN_MENU) {
                audioManager.playButtonClick();
                transitionToState(GameState.MAIN_MENU);
                return;
            }
            mainMenu.update(dt, inputManager, gamePanel.getWidth(), gamePanel.getHeight(), this);
            return;
        }

        // SETTINGS SCREEN
        if (state == GameState.SETTINGS) {
            if (inputManager.consumeKeyPress(KeyEvent.VK_ESCAPE)) {
                config.saveToFile();
                audioManager.playButtonClick();
                transitionToState(GameState.MAIN_MENU);
                return;
            }
            settingsScreen.update(dt, inputManager, gamePanel.getWidth(), gamePanel.getHeight(), this);
            return;
        }

        // LOBBY STATE
        if (state == GameState.LOBBY) {
            if (inputManager.consumeKeyPress(KeyEvent.VK_ESCAPE)) {
                audioManager.playButtonClick();
                networkManager.disconnect();
                transitionToState(GameState.MAIN_MENU);
                return;
            }
            lobbyScreen.update(dt, inputManager, gamePanel.getWidth(), gamePanel.getHeight(), this);
            return;
        }

        // GAME_OVER STATE
        if (state == GameState.GAME_OVER) {
            if (!networkManager.isConnected() && inputManager.consumeKeyPress(KeyEvent.VK_SPACE)) {
                audioManager.playButtonClick();
                startOfflineMatchFromConfig();
                return;
            }
            if (inputManager.consumeKeyPress(KeyEvent.VK_ESCAPE)) {
                audioManager.playButtonClick();
                networkManager.disconnect();
                transitionToState(GameState.MAIN_MENU);
                return;
            }
            return;
        }

        // EJECTION STATE
        if (state == GameState.EJECTION) {
            boolean done = ejectionScreen.update(dt) || (!networkManager.isConnected() && inputManager.consumeKeyPress(KeyEvent.VK_SPACE));
            if (done && !networkManager.isConnected()) {
                WinConditionManager.MatchOutcome outcome = winConditionManager.evaluate(players, taskManager, sabotageManager);
                if (outcome != null) {
                    triggerGameOver(outcome);
                } else {
                    for (AIController ai : aiControllers) {
                        ai.onMeetingEnded();
                    }
                    transitionToState(GameState.PLAYING);
                    camera.snapTo(localPlayer.getCharacter().getX(), localPlayer.getCharacter().getY());
                }
            }
            return;
        }

        // MEETING STATE
        if (state == GameState.MEETING) {
            meetingScreen.update(
                    inputManager, gamePanel.getWidth(), gamePanel.getHeight(),
                    players, localPlayer, config, meetingManager, votingManager, networkManager
            );
            if (networkManager.isConnected()) {
                meetingManager.updateNetworkTimers(dt, config);
            } else {
                boolean meetingEnded = meetingManager.update(dt, players, aiControllers, config, votingManager);
                if (meetingEnded) {
                    ejectionScreen.start(votingManager, players);
                    transitionToState(GameState.EJECTION);
                }
            }
            return;
        }

        // PLAYING STATE
        if (!networkManager.isConnected() && inputManager.consumeKeyPress(KeyEvent.VK_F2)) {
            startOfflineMatchFromConfig();
            return;
        }
        if (!networkManager.isConnected() && inputManager.consumeKeyPress(KeyEvent.VK_TAB)) {
            boolean switchToImp = localPlayer.getCharacter().getOriginalRole() != PlayerRole.IMPOSTOR;
            startNewMatch(switchToImp);
            return;
        }

        // Surveillance (Admin Map / Security Cameras) modal handling
        if (surveillanceOverlay.isOpen()) {
            surveillanceOverlay.update(inputManager, gamePanel.getWidth(), gamePanel.getHeight());
            updateWorldAndAI(dt);
            camera.follow(localPlayer.getCharacter().getX(), localPlayer.getCharacter().getY(), dt);
            renderer.update(dt, gamePanel.getWidth(), gamePanel.getHeight(), inputManager);
            if (!networkManager.isConnected()) checkMatchOutcome();
            return;
        }

        // Active Task Mini-Game modal handling
        if (taskManager.hasActiveTaskOpen()) {
            if (inputManager.consumeKeyPress(KeyEvent.VK_ESCAPE)) {
                taskManager.closeActiveTask();
                return;
            }
            taskManager.update(dt, inputManager, gamePanel.getWidth(), gamePanel.getHeight(), localPlayer, gameMap);
            String completedTaskId = taskManager.consumeLastCompletedTaskId();
            if (completedTaskId != null) {
                audioManager.playTaskComplete();
                Character lc = localPlayer.getCharacter();
                particleSystem.spawnTaskSparkles(lc.getX(), lc.getY());
                if (networkManager.isConnected()) {
                    networkManager.sendTaskComplete(completedTaskId);
                }
            }
            updateWorldAndAI(dt);
            renderer.update(dt, gamePanel.getWidth(), gamePanel.getHeight(), inputManager);
            if (!networkManager.isConnected()) checkMatchOutcome();
            return;
        }

        if (inputManager.consumeKeyPress(KeyEvent.VK_ESCAPE)) {
            audioManager.playButtonClick();
            networkManager.disconnect();
            transitionToState(GameState.MAIN_MENU);
            return;
        }

        // Cosmetics & Ghost hotkeys
        if (inputManager.consumeKeyPress(KeyEvent.VK_C)) { localPlayer.cycleColor(); audioManager.playButtonClick(); }
        if (inputManager.consumeKeyPress(KeyEvent.VK_H)) { localPlayer.cycleHat(); audioManager.playButtonClick(); }
        if (inputManager.consumeKeyPress(KeyEvent.VK_K)) { localPlayer.cycleSkin(); audioManager.playButtonClick(); }
        if (inputManager.consumeKeyPress(KeyEvent.VK_P)) { localPlayer.cyclePet(); audioManager.playButtonClick(); }
        if (!networkManager.isConnected() && inputManager.consumeKeyPress(KeyEvent.VK_G)) {
            Character c = localPlayer.getCharacter();
            if (c.getRole() == PlayerRole.GHOST) {
                c.reviveAt(c.getX(), c.getY(), PlayerRole.CREWMATE);
            } else {
                c.becomeGhost();
            }
        }

        Character localChar = localPlayer.getCharacter();
        meetingManager.updateCooldown(dt);

        // Dead Body Reporting [R]
        reportableBody = meetingManager.findReportableBody(localChar, players);
        if (inputManager.consumeKeyPress(KeyEvent.VK_R) && reportableBody != null) {
            if (networkManager.isConnected()) {
                networkManager.sendBodyReportRequest(reportableBody.getId());
            } else {
                enterBodyReportMeeting(localPlayer, reportableBody);
            }
            return;
        }

        // Impostor controls (when alive)
        if (localChar instanceof Impostor imp && imp.isAlive()) {
            imp.updateCooldowns(dt);
            killTarget = killManager.findNearestKillTarget(imp, players);
            nearVent = gameMap.findNearestVent(imp.getX(), imp.getY(), imp.getInteractionRange()) != null;

            if (inputManager.consumeKeyPress(KeyEvent.VK_Q) && killTarget != null) {
                if (networkManager.isConnected()) {
                    networkManager.sendKillRequest(killTarget.getId());
                } else {
                    Character victim = killTarget;
                    if (killManager.executeKill(imp, victim)) {
                        notifyAllAIsOfKill(imp, victim);
                    }
                    checkMatchOutcome();
                    if (state == GameState.GAME_OVER) return;
                }
            }
            if (inputManager.consumeKeyPress(KeyEvent.VK_V)) {
                if (networkManager.isConnected()) {
                    networkManager.sendVentToggle();
                    audioManager.playVent();
                } else if (ventManager.toggleVent(imp, gameMap)) {
                    audioManager.playVent();
                    particleSystem.spawnVentPuff(imp.getX(), imp.getY());
                }
            }
            if (imp.isInsideVent()) {
                if (inputManager.consumeKeyPress(KeyEvent.VK_A) || inputManager.consumeKeyPress(KeyEvent.VK_LEFT)) {
                    if (networkManager.isConnected()) networkManager.sendVentCycle(-1);
                    else if (ventManager.cycleVent(imp, gameMap, -1)) audioManager.playVent();
                } else if (inputManager.consumeKeyPress(KeyEvent.VK_D) || inputManager.consumeKeyPress(KeyEvent.VK_RIGHT)) {
                    if (networkManager.isConnected()) networkManager.sendVentCycle(1);
                    else if (ventManager.cycleVent(imp, gameMap, 1)) audioManager.playVent();
                }
            }

            if (inputManager.consumeKeyPress(KeyEvent.VK_1)) triggerSabotageAction(SabotageType.REACTOR_MELTDOWN);
            if (inputManager.consumeKeyPress(KeyEvent.VK_2)) triggerSabotageAction(SabotageType.OXYGEN_DEPLETION);
            if (inputManager.consumeKeyPress(KeyEvent.VK_3)) triggerSabotageAction(SabotageType.LIGHTS_SABOTAGE);
            if (inputManager.consumeKeyPress(KeyEvent.VK_4)) triggerSabotageAction(SabotageType.COMMUNICATIONS_SABOTAGE);
            if (inputManager.consumeKeyPress(KeyEvent.VK_5)) triggerSabotageAction(SabotageType.DOOR_LOCKDOWN);
        } else {
            killTarget = null;
            nearVent = false;
        }

        // Local Player Movement & Footstep Audio
        boolean canWalk = !(localChar instanceof Impostor imp && imp.isAlive() && imp.isInsideVent());
        if (canWalk) {
            double dx = (inputManager.isRightPressed() ? 1 : 0) - (inputManager.isLeftPressed() ? 1 : 0);
            double dy = (inputManager.isDownPressed() ? 1 : 0) - (inputManager.isUpPressed() ? 1 : 0);
            if (dx != 0 || dy != 0) {
                double len = Math.hypot(dx, dy);
                double speed = localChar.getMoveSpeed();
                localChar.setVelocity((dx / len) * speed, (dy / len) * speed);

                if (localChar.isAlive()) {
                    footstepTimer += dt;
                    if (footstepTimer >= 0.34) {
                        footstepTimer = 0.0;
                        audioManager.playFootstep();
                    }
                }
            } else {
                localChar.setVelocity(0, 0);
                footstepTimer = 0.22;
            }
        } else {
            localChar.setVelocity(0, 0);
        }

        updateWorldAndAI(dt);
        if (state == GameState.MEETING || state == GameState.GAME_OVER) return;

        RoomId currentRoom = localChar.getCurrentRoom();
        if (currentRoom != RoomId.CORRIDOR && currentRoom != lastAnnouncedRoom) {
            lastAnnouncedRoom = currentRoom;
            roomBannerAlpha = 1.4f;
        } else if (roomBannerAlpha > 0f) {
            roomBannerAlpha = Math.max(0f, roomBannerAlpha - (float) (dt * 0.75));
        }

        nearbyInteractable = gameMap.findNearestInteractable(
                localChar.getX(), localChar.getY(), localChar.getInteractionRange());

        if (inputManager.consumeKeyPress(KeyEvent.VK_E) && nearbyInteractable != null) {
            handleInteraction(localChar, nearbyInteractable);
        }

        camera.follow(localChar.getX(), localChar.getY(), dt);
        renderer.update(dt, gamePanel.getWidth(), gamePanel.getHeight(), inputManager);
        if (!networkManager.isConnected()) checkMatchOutcome();
    }

    public void openMultiplayerLobby(boolean autoHost) {
        taskManager.closeActiveTask();
        surveillanceOverlay.close();
        aiControllers.clear();
        players.clear();
        particleSystem.clear();

        Character lc = localPlayer.getCharacter();
        Character lobbyChar = new Crewmate(networkManager.getLocalClientId(), lc.getName(), 1630, 465, lc.getColor());
        lobbyChar.setHat(lc.getHat());
        lobbyChar.setSkin(lc.getSkin());
        lobbyChar.setPet(lc.getPet());
        this.localPlayer = new Player(networkManager.getLocalClientId(), lobbyChar, true, false);
        players.add(localPlayer);

        lobbyScreen.onEnter(lobbyChar, config);
        inputManager.clearTypedCharacters();
        transitionToState(GameState.LOBBY);

        if (autoHost) {
            networkManager.hostGame(config, lobbyChar, config.isPrivateRoom());
        }
    }

    public void applyNetworkLobbyRoster(String localId, String rawRoster) {
        if (rawRoster == null || rawRoster.isBlank()) return;
        Map<String, Player> existingById = new HashMap<>();
        for (Player p : players) existingById.put(p.getPlayerId(), p);

        List<Player> updated = new ArrayList<>();
        for (String entry : rawRoster.split(";")) {
            String[] f = entry.split(",");
            if (f.length < 7) continue;
            String id = f[0];
            String name = f[1];
            PlayerColor col = parseEnum(PlayerColor.class, f[2], PlayerColor.CYAN);
            Cosmetics.Hat hat = parseEnum(Cosmetics.Hat.class, f[3], Cosmetics.Hat.NONE);
            Cosmetics.Skin skin = parseEnum(Cosmetics.Skin.class, f[4], Cosmetics.Skin.DEFAULT);
            Cosmetics.Pet pet = parseEnum(Cosmetics.Pet.class, f[5], Cosmetics.Pet.NONE);
            boolean conn = Boolean.parseBoolean(f[6]);

            Player p = existingById.get(id);
            if (p == null) {
                Character c = new Crewmate(id, name, 1630, 465, col);
                p = new Player(id, c, id.equals(localId), false);
            }
            Character c = p.getCharacter();
            if (!p.isLocalPlayer()) {
                c.setName(name);
                c.setColor(col);
                c.setHat(hat);
                c.setSkin(skin);
                c.setPet(pet);
            }
            c.setConnected(conn);
            if (p.isLocalPlayer()) {
                this.localPlayer = p;
            }
            updated.add(p);
        }
        players.clear();
        players.addAll(updated);
    }

    public void startNetworkMatchFromServer(String localId, PlayerRole myRole, String rawPlayers) {
        players.clear();
        aiControllers.clear();
        particleSystem.clear();
        sabotageManager.reset();
        meetingManager.reset(config);
        votingManager.reset();
        taskManager.closeActiveTask();
        surveillanceOverlay.close();
        matchOutcome = null;

        int crewCount = 0;
        for (String entry : rawPlayers.split(";")) {
            String[] f = entry.split(",");
            if (f.length < 9) continue;
            String id = f[0];
            String name = f[1];
            PlayerColor col = parseEnum(PlayerColor.class, f[2], PlayerColor.CYAN);
            Cosmetics.Hat hat = parseEnum(Cosmetics.Hat.class, f[3], Cosmetics.Hat.NONE);
            Cosmetics.Skin skin = parseEnum(Cosmetics.Skin.class, f[4], Cosmetics.Skin.DEFAULT);
            Cosmetics.Pet pet = parseEnum(Cosmetics.Pet.class, f[5], Cosmetics.Pet.NONE);
            PlayerRole role = id.equals(localId) ? myRole : parseEnum(PlayerRole.class, f[6], PlayerRole.CREWMATE);
            double x = Double.parseDouble(f[7]);
            double y = Double.parseDouble(f[8]);

            if (role != PlayerRole.IMPOSTOR) crewCount++;
            Character c = (role == PlayerRole.IMPOSTOR)
                    ? new Impostor(id, name, x, y, col)
                    : new Crewmate(id, name, x, y, col);
            c.setHat(hat);
            c.setSkin(skin);
            c.setPet(pet);

            Player p = new Player(id, c, id.equals(localId), false);
            if (p.isLocalPlayer()) this.localPlayer = p;
            players.add(p);
        }

        taskManager.assignTasksForMatch(gameMap, 5, Math.max(0, crewCount - 1));
        taskManager.setNetworkTaskProgress(0.0f);
        transitionToState(GameState.PLAYING);
        camera.snapTo(localPlayer.getCharacter().getX(), localPlayer.getCharacter().getY());
    }

    public void applyNetworkStateSync(String localId, String rawStates) {
        if (rawStates == null || rawStates.isBlank()) return;
        for (String entry : rawStates.split(";")) {
            String[] f = entry.split(",");
            if (f.length < 8) continue;
            String id = f[0];
            double x = Double.parseDouble(f[1]);
            double y = Double.parseDouble(f[2]);
            boolean facingRight = Boolean.parseBoolean(f[3]);
            MovementState mState = parseEnum(MovementState.class, f[4], MovementState.IDLE);
            boolean alive = Boolean.parseBoolean(f[5]);
            boolean conn = Boolean.parseBoolean(f[6]);
            boolean inVent = Boolean.parseBoolean(f[7]);

            for (Player p : players) {
                if (p.getPlayerId().equals(id)) {
                    Character c = p.getCharacter();
                    c.setConnected(conn);
                    if (!alive && c.isAlive()) {
                        c.kill();
                    }
                    if (c instanceof Impostor imp) {
                        imp.setInsideVent(inVent);
                    }
                    if (!id.equals(localId) || inVent) {
                        c.setX(x);
                        c.setY(y);
                        c.setFacingRight(facingRight);
                        c.setMovementState(mState);
                    }
                    break;
                }
            }
        }
    }

    public void applyNetworkKill(String killerId, String victimId, double vx, double vy) {
        for (Player p : players) {
            Character c = p.getCharacter();
            if (p.getPlayerId().equals(killerId)) {
                c.setX(vx);
                c.setY(vy);
                if (c instanceof Impostor imp) imp.triggerKillCooldown();
            } else if (p.getPlayerId().equals(victimId)) {
                c.setX(vx);
                c.setY(vy);
                c.kill();
                audioManager.playKill();
                particleSystem.spawnKillBurst(vx, vy, c.getColor().getMainColor());
            }
        }
    }

    public void applyNetworkMeetingStart(String callerId, String headline) {
        taskManager.closeActiveTask();
        surveillanceOverlay.close();
        inputManager.clearTypedCharacters();
        meetingScreen.resetInput();
        audioManager.playEmergencyMeeting();
        meetingManager.startNetworkMeeting(callerId, headline, players, config, sabotageManager, votingManager);
        transitionToState(GameState.MEETING);
    }

    public void addNetworkChatMessage(String senderName, Color color, String text) {
        meetingManager.addRawChatMessage(senderName, color, text);
    }

    public void applyNetworkVoteResult(String ejectedId, boolean wasTie, boolean wasSkipped, String rawPairs) {
        votingManager.applyServerResult(ejectedId, wasTie, wasSkipped, rawPairs);
        ejectionScreen.start(votingManager, players);
        transitionToState(GameState.EJECTION);
    }

    public void applyNetworkGameOver(WinConditionManager.MatchOutcome outcome) {
        triggerGameOver(outcome);
    }

    private void triggerSabotageAction(SabotageType type) {
        if (networkManager.isConnected()) {
            networkManager.sendSabotageRequest(type);
            audioManager.playSabotageAlarm();
        } else if (sabotageManager.triggerSabotage(type, gameMap)) {
            audioManager.playSabotageAlarm();
        }
    }

    private void updateWorldAndAI(double dt) {
        gameMap.update(dt);
        particleSystem.update(dt);

        if (sabotageManager.getActiveSabotage() != SabotageType.NONE) {
            sabotageSparkTimer += dt;
            if (sabotageSparkTimer >= 0.45) {
                sabotageSparkTimer = 0.0;
                for (InteractiveObject obj : gameMap.getAllInteractables()) {
                    if (sabotageManager.canRepairWithInteractable(obj.getId())) {
                        particleSystem.spawnSabotageSparks(obj.getX(), obj.getY());
                    }
                }
            }
        }

        if (!networkManager.isConnected()) {
            sabotageManager.update(dt);
            for (AIController ai : aiControllers) {
                if (ai.getPlayer().getCharacter() instanceof Impostor imp) {
                    imp.updateCooldowns(dt);
                }
                ai.update(dt, this);
                if (state == GameState.MEETING) return;
            }
        }

        for (Player p : players) {
            Character c = p.getCharacter();
            if (p.isLocalPlayer() || !networkManager.isConnected()) {
                c.update(dt, gameMap.getCollisionManager());
            } else {
                c.update(dt, null);
            }
            c.setCurrentRoom(gameMap.getRoomAt(c.getX(), c.getY()));
        }
    }

    public void notifyAllAIsOfKill(Character killer, Character victim) {
        audioManager.playKill();
        particleSystem.spawnKillBurst(victim.getDeadBodyX(), victim.getDeadBodyY(), victim.getColor().getMainColor());
        for (AIController ai : aiControllers) {
            ai.notifyKillObserved(killer, victim, players);
        }
    }

    public void triggerAIBodyReport(Player reporter, Character deadBody) {
        if (state != GameState.PLAYING) return;
        enterBodyReportMeeting(reporter, deadBody);
    }

    private void enterBodyReportMeeting(Player reporter, Character deadBody) {
        taskManager.closeActiveTask();
        surveillanceOverlay.close();
        inputManager.clearTypedCharacters();
        meetingScreen.resetInput();
        for (AIController ai : aiControllers) {
            ai.onMeetingStarted();
        }
        if (meetingManager.startBodyReportMeeting(reporter, deadBody, players, config, sabotageManager, votingManager)) {
            audioManager.playEmergencyMeeting();
            transitionToState(GameState.MEETING);
        }
    }

    private void handleInteraction(Character localChar, InteractiveObject obj) {
        if (obj.getType() == InteractiveObject.InteractionType.EMERGENCY_BUTTON) {
            if (networkManager.isConnected()) {
                networkManager.sendEmergencyMeetingRequest();
            } else {
                inputManager.clearTypedCharacters();
                meetingScreen.resetInput();
                for (AIController ai : aiControllers) {
                    ai.onMeetingStarted();
                }
                if (meetingManager.startEmergencyMeeting(localPlayer, players, config, sabotageManager, votingManager)) {
                    audioManager.playEmergencyMeeting();
                    transitionToState(GameState.MEETING);
                }
            }
            return;
        }

        if (sabotageManager.canRepairWithInteractable(obj.getId())) {
            if (networkManager.isConnected()) {
                networkManager.sendSabotageRepair();
            } else {
                sabotageManager.repairActiveSabotage();
            }
            audioManager.playTaskComplete();
            particleSystem.spawnTaskSparkles(obj.getX(), obj.getY());
            return;
        }

        if (localChar instanceof Impostor imp && imp.isAlive() && imp.isInsideVent()) {
            if (networkManager.isConnected()) networkManager.sendVentToggle();
            else ventManager.toggleVent(imp, gameMap);
            audioManager.playVent();
            return;
        }

        if (obj.getType() == InteractiveObject.InteractionType.ADMIN_MAP) {
            surveillanceOverlay.openAdminMap();
            audioManager.playButtonClick();
            return;
        }

        if (obj.getType() == InteractiveObject.InteractionType.SECURITY_CAMERAS) {
            surveillanceOverlay.openSecurityCams();
            audioManager.playButtonClick();
            return;
        }

        if (localChar.getOriginalRole() != PlayerRole.IMPOSTOR && obj.getType() == InteractiveObject.InteractionType.TASK_TERMINAL) {
            if (taskManager.tryOpenTaskForInteractable(obj.getId())) {
                audioManager.playButtonClick();
            }
        }
    }

    private void checkMatchOutcome() {
        if (state == GameState.GAME_OVER) return;
        WinConditionManager.MatchOutcome outcome = winConditionManager.evaluate(players, taskManager, sabotageManager);
        if (outcome != null) {
            triggerGameOver(outcome);
        }
    }

    private void triggerGameOver(WinConditionManager.MatchOutcome outcome) {
        this.matchOutcome = outcome;
        transitionToState(GameState.GAME_OVER);
        taskManager.closeActiveTask();
        surveillanceOverlay.close();

        PlayerRole localRole = localPlayer.getCharacter().getOriginalRole();
        boolean won = (outcome.getWinner() == WinConditionManager.Faction.CREWMATES && localRole == PlayerRole.CREWMATE)
                || (outcome.getWinner() == WinConditionManager.Faction.IMPOSTORS && localRole == PlayerRole.IMPOSTOR);
        if (won) audioManager.playVictory();
        else audioManager.playDefeat();

        Logger.warn("MATCH OVER -> " + outcome.getHeadline() + ": " + outcome.getReason());
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String val, E fallback) {
        try {
            return Enum.valueOf(enumType, val.trim().toUpperCase());
        } catch (Exception e) {
            return fallback;
        }
    }

    public void render(Graphics2D g2, int width, int height) {
        renderer.render(g2, width, height, this);
    }

    public void shutdown() {
        Logger.info("Shutting down GameEngine...");
        config.saveToFile();
        networkManager.disconnect();
        audioManager.stop();
        gameLoop.stop();
        inputManager.clearAll();
    }

    public GameConfig getConfig() { return config; }
    public InputManager getInputManager() { return inputManager; }
    public GameMap getGameMap() { return gameMap; }
    public Camera getCamera() { return camera; }
    public AudioManager getAudioManager() { return audioManager; }
    public ParticleSystem getParticleSystem() { return particleSystem; }
    public Player getLocalPlayer() { return localPlayer; }
    public List<Player> getPlayers() { return Collections.unmodifiableList(players); }
    public List<AIController> getAiControllers() { return Collections.unmodifiableList(aiControllers); }
    public TaskManager getTaskManager() { return taskManager; }
    public KillManager getKillManager() { return killManager; }
    public VentManager getVentManager() { return ventManager; }
    public SabotageManager getSabotageManager() { return sabotageManager; }
    public MeetingManager getMeetingManager() { return meetingManager; }
    public VotingManager getVotingManager() { return votingManager; }
    public NetworkManager getNetworkManager() { return networkManager; }
    public MainMenu getMainMenu() { return mainMenu; }
    public SettingsScreen getSettingsScreen() { return settingsScreen; }
    public LobbyScreen getLobbyScreen() { return lobbyScreen; }
    public MeetingScreen getMeetingScreen() { return meetingScreen; }
    public EjectionScreen getEjectionScreen() { return ejectionScreen; }
    public GameOverScreen getGameOverScreen() { return gameOverScreen; }
    public SurveillanceOverlay getSurveillanceOverlay() { return surveillanceOverlay; }
    public WinConditionManager.MatchOutcome getMatchOutcome() { return matchOutcome; }
    public GameState getState() { return state; }
    public void setState(GameState state) { this.state = state; }
    public float getScreenFadeAlpha() { return screenFadeAlpha; }
    public float getRoomBannerAlpha() { return roomBannerAlpha; }
    public InteractiveObject getNearbyInteractable() { return nearbyInteractable; }
    public Character getKillTarget() { return killTarget; }
    public Character getReportableBody() { return reportableBody; }
    public boolean isNearVent() { return nearVent; }
    public int getCurrentFps() { return gameLoop.getCurrentFps(); }
    public double getLastDeltaSeconds() { return gameLoop.getLastDeltaSeconds(); }
}