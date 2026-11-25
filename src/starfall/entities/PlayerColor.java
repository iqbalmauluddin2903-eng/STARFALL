package starfall.entities;

import java.awt.Color;

public enum PlayerColor {
    RED("Red", new Color(198, 24, 24), new Color(125, 12, 18)),
    BLUE("Blue", new Color(25, 65, 215), new Color(14, 34, 130)),
    GREEN("Green", new Color(22, 138, 48), new Color(12, 82, 32)),
    PINK("Pink", new Color(238, 88, 188), new Color(165, 45, 132)),
    ORANGE("Orange", new Color(242, 130, 18), new Color(168, 76, 8)),
    YELLOW("Yellow", new Color(246, 242, 88), new Color(180, 165, 32)),
    BLACK("Black", new Color(64, 72, 82), new Color(34, 38, 46)),
    WHITE("White", new Color(216, 226, 240), new Color(145, 158, 178)),
    PURPLE("Purple", new Color(112, 52, 192), new Color(64, 24, 122)),
    BROWN("Brown", new Color(118, 76, 36), new Color(72, 44, 20)),
    CYAN("Cyan", new Color(56, 248, 220), new Color(28, 164, 168)),
    LIME("Lime", new Color(84, 238, 62), new Color(42, 158, 34));

    private final String displayName;
    private final Color mainColor;
    private final Color shadowColor;

    PlayerColor(String displayName, Color mainColor, Color shadowColor) {
        this.displayName = displayName;
        this.mainColor = mainColor;
        this.shadowColor = shadowColor;
    }

    public String getDisplayName() { return displayName; }
    public Color getMainColor() { return mainColor; }
    public Color getShadowColor() { return shadowColor; }

    public PlayerColor next() {
        PlayerColor[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}