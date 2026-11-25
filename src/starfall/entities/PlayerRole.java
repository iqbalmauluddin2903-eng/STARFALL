package starfall.entities;

public enum PlayerRole {
    CREWMATE("Crewmate"),
    IMPOSTOR("Impostor"),
    GHOST("Ghost");

    private final String displayName;

    PlayerRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}