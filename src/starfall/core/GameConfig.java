package starfall.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import starfall.utils.Logger;

public class GameConfig {

    public static final String GAME_TITLE = "STARFALL: SPACE MYSTERY";
    public static final String VERSION = "1.0.0-Release";
    private static final Path CONFIG_FILE = Path.of("config", "starfall.properties");

    public enum GraphicsQuality {
        HIGH("HIGH"),
        MEDIUM("MEDIUM"),
        LOW("LOW");

        private final String label;
        GraphicsQuality(String label) { this.label = label; }
        public String getLabel() { return label; }
        public GraphicsQuality next() {
            GraphicsQuality[] vals = values();
            return vals[(ordinal() + 1) % vals.length];
        }
    }

    public enum Language {
        EN("ENGLISH"),
        ID("INDONESIA");

        private final String label;
        Language(String label) { this.label = label; }
        public String getLabel() { return label; }
        public Language next() {
            return this == EN ? ID : EN;
        }
    }

    public enum ControlScheme {
        WASD_AND_ARROWS("WASD + ARROWS"),
        WASD_ONLY("WASD ONLY");

        private final String label;
        ControlScheme(String label) { this.label = label; }
        public String getLabel() { return label; }
        public ControlScheme next() {
            return this == WASD_AND_ARROWS ? WASD_ONLY : WASD_AND_ARROWS;
        }
    }

    public static final int[][] RESOLUTION_PRESETS = {
        {1280, 720},
        {1366, 768},
        {1600, 900}
    };

    private int resolutionIndex = 0;
    private int windowWidth = 1280;
    private int windowHeight = 720;
    private int targetFps = 60;
    private boolean fullscreen = false;
    private boolean showDebugHud = false;

    private GraphicsQuality graphicsQuality = GraphicsQuality.HIGH;
    private Language language = Language.EN;
    private ControlScheme controlScheme = ControlScheme.WASD_AND_ARROWS;

    private boolean anonymousVoting = false;
    private int emergencyMeetingsPerPlayer = 2;
    private double emergencyCooldownSeconds = 12.0;
    private double discussionTimeSeconds = 6.0;
    private double votingTimeSeconds = 35.0;

    private int offlinePlayerCount = 7;
    private String defaultHostIp = "127.0.0.1";
    private int serverPort = 25565;
    private int impostorCount = 1;
    private int maxPlayers = 8;
    private boolean privateRoom = true;

    private float masterVolume = 0.8f;
    private float musicVolume = 0.5f;
    private float sfxVolume = 0.85f;

    public GameConfig() {
        loadFromFile();
    }

    public void loadFromFile() {
        if (!Files.exists(CONFIG_FILE)) return;
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
            props.load(in);
            masterVolume = clamp01(Float.parseFloat(props.getProperty("masterVolume", "0.8")));
            musicVolume = clamp01(Float.parseFloat(props.getProperty("musicVolume", "0.5")));
            sfxVolume = clamp01(Float.parseFloat(props.getProperty("sfxVolume", "0.85")));
            resolutionIndex = Math.max(0, Math.min(RESOLUTION_PRESETS.length - 1,
                    Integer.parseInt(props.getProperty("resolutionIndex", "0"))));
            windowWidth = RESOLUTION_PRESETS[resolutionIndex][0];
            windowHeight = RESOLUTION_PRESETS[resolutionIndex][1];
            showDebugHud = Boolean.parseBoolean(props.getProperty("showDebugHud", "false"));
            anonymousVoting = Boolean.parseBoolean(props.getProperty("anonymousVoting", "false"));
            offlinePlayerCount = Math.max(4, Math.min(10, Integer.parseInt(props.getProperty("offlinePlayerCount", "7"))));
            impostorCount = Math.max(1, Math.min(3, Integer.parseInt(props.getProperty("impostorCount", "1"))));
            graphicsQuality = GraphicsQuality.valueOf(props.getProperty("graphicsQuality", "HIGH"));
            language = Language.valueOf(props.getProperty("language", "EN"));
            controlScheme = ControlScheme.valueOf(props.getProperty("controlScheme", "WASD_AND_ARROWS"));
            defaultHostIp = props.getProperty("defaultHostIp", "127.0.0.1");
            serverPort = Integer.parseInt(props.getProperty("serverPort", "25565"));
            Logger.info("Loaded settings from " + CONFIG_FILE);
        } catch (Exception e) {
            Logger.warn("Using default GameConfig: " + e.getMessage());
        }
    }

    public void saveToFile() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            Properties props = new Properties();
            props.setProperty("masterVolume", String.valueOf(masterVolume));
            props.setProperty("musicVolume", String.valueOf(musicVolume));
            props.setProperty("sfxVolume", String.valueOf(sfxVolume));
            props.setProperty("resolutionIndex", String.valueOf(resolutionIndex));
            props.setProperty("showDebugHud", String.valueOf(showDebugHud));
            props.setProperty("anonymousVoting", String.valueOf(anonymousVoting));
            props.setProperty("offlinePlayerCount", String.valueOf(offlinePlayerCount));
            props.setProperty("impostorCount", String.valueOf(impostorCount));
            props.setProperty("graphicsQuality", graphicsQuality.name());
            props.setProperty("language", language.name());
            props.setProperty("controlScheme", controlScheme.name());
            props.setProperty("defaultHostIp", defaultHostIp);
            props.setProperty("serverPort", String.valueOf(serverPort));
            try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
                props.store(out, "STARFALL: SPACE MYSTERY Configuration");
            }
        } catch (IOException e) {
            Logger.warn("Failed to save GameConfig: " + e.getMessage());
        }
    }

    public void cycleResolution() {
        resolutionIndex = (resolutionIndex + 1) % RESOLUTION_PRESETS.length;
        windowWidth = RESOLUTION_PRESETS[resolutionIndex][0];
        windowHeight = RESOLUTION_PRESETS[resolutionIndex][1];
    }

    public String getResolutionLabel() {
        return windowWidth + "x" + windowHeight;
    }

    public int getWindowWidth() { return windowWidth; }
    public void setWindowWidth(int windowWidth) { this.windowWidth = windowWidth; }

    public int getWindowHeight() { return windowHeight; }
    public void setWindowHeight(int windowHeight) { this.windowHeight = windowHeight; }

    public int getTargetFps() { return targetFps; }
    public void setTargetFps(int targetFps) { this.targetFps = Math.max(30, targetFps); }

    public boolean isFullscreen() { return fullscreen; }
    public void setFullscreen(boolean fullscreen) { this.fullscreen = fullscreen; }

    public boolean isShowDebugHud() { return showDebugHud; }
    public void setShowDebugHud(boolean showDebugHud) { this.showDebugHud = showDebugHud; }
    public void toggleDebugHud() { this.showDebugHud = !this.showDebugHud; }

    public GraphicsQuality getGraphicsQuality() { return graphicsQuality; }
    public void setGraphicsQuality(GraphicsQuality graphicsQuality) { this.graphicsQuality = graphicsQuality; }

    public Language getLanguage() { return language; }
    public void setLanguage(Language language) { this.language = language; }

    public ControlScheme getControlScheme() { return controlScheme; }
    public void setControlScheme(ControlScheme controlScheme) { this.controlScheme = controlScheme; }

    public boolean isAnonymousVoting() { return anonymousVoting; }
    public void setAnonymousVoting(boolean anonymousVoting) { this.anonymousVoting = anonymousVoting; }
    public void toggleAnonymousVoting() { this.anonymousVoting = !this.anonymousVoting; }

    public int getEmergencyMeetingsPerPlayer() { return emergencyMeetingsPerPlayer; }
    public void setEmergencyMeetingsPerPlayer(int count) { this.emergencyMeetingsPerPlayer = Math.max(1, count); }

    public double getEmergencyCooldownSeconds() { return emergencyCooldownSeconds; }
    public void setEmergencyCooldownSeconds(double sec) { this.emergencyCooldownSeconds = Math.max(0.0, sec); }

    public double getDiscussionTimeSeconds() { return discussionTimeSeconds; }
    public void setDiscussionTimeSeconds(double sec) { this.discussionTimeSeconds = Math.max(0.0, sec); }

    public double getVotingTimeSeconds() { return votingTimeSeconds; }
    public void setVotingTimeSeconds(double sec) { this.votingTimeSeconds = Math.max(10.0, sec); }

    public int getOfflinePlayerCount() { return offlinePlayerCount; }
    public void setOfflinePlayerCount(int count) { this.offlinePlayerCount = Math.max(4, Math.min(10, count)); }

    public String getDefaultHostIp() { return defaultHostIp; }
    public void setDefaultHostIp(String defaultHostIp) { this.defaultHostIp = defaultHostIp; }

    public int getServerPort() { return serverPort; }
    public void setServerPort(int serverPort) { this.serverPort = Math.max(1024, Math.min(65535, serverPort)); }

    public int getImpostorCount() { return impostorCount; }
    public void setImpostorCount(int impostorCount) { this.impostorCount = Math.max(1, Math.min(3, impostorCount)); }

    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = Math.max(2, Math.min(10, maxPlayers)); }

    public boolean isPrivateRoom() { return privateRoom; }
    public void setPrivateRoom(boolean privateRoom) { this.privateRoom = privateRoom; }

    public float getMasterVolume() { return masterVolume; }
    public void setMasterVolume(float masterVolume) { this.masterVolume = clamp01(masterVolume); }

    public float getMusicVolume() { return musicVolume; }
    public void setMusicVolume(float musicVolume) { this.musicVolume = clamp01(musicVolume); }

    public float getSfxVolume() { return sfxVolume; }
    public void setSfxVolume(float sfxVolume) { this.sfxVolume = clamp01(sfxVolume); }

    private float clamp01(float val) {
        return Math.max(0f, Math.min(1f, val));
    }
}