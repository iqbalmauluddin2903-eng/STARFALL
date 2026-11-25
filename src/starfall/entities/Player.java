package starfall.entities;

public class Player {

    private final String playerId;
    private final boolean localPlayer;
    private final boolean aiControlled;
    private Character character;

    public Player(String playerId, Character character, boolean localPlayer, boolean aiControlled) {
        this.playerId = playerId;
        this.character = character;
        this.localPlayer = localPlayer;
        this.aiControlled = aiControlled;
    }

    public String getPlayerId() { return playerId; }
    public boolean isLocalPlayer() { return localPlayer; }
    public boolean isAiControlled() { return aiControlled; }

    public Character getCharacter() { return character; }
    public void setCharacter(Character character) { this.character = character; }

    public void cycleColor() {
        character.setColor(character.getColor().next());
    }

    public void cycleHat() {
        character.setHat(character.getHat().next());
    }

    public void cycleSkin() {
        character.setSkin(character.getSkin().next());
    }

    public void cyclePet() {
        character.setPet(character.getPet().next());
    }
}