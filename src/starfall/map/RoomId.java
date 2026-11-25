package starfall.map;

public enum RoomId {
    CAFETERIA("CAFETERIA"),
    WEAPONS("WEAPONS"),
    O2("O2"),
    NAVIGATION("NAVIGATION"),
    SHIELDS("SHIELDS"),
    COMMUNICATIONS("COMMUNICATIONS"),
    STORAGE("STORAGE"),
    ADMIN("ADMIN"),
    ELECTRICAL("ELECTRICAL"),
    LOWER_ENGINE("LOWER ENGINE"),
    SECURITY("SECURITY"),
    REACTOR("REACTOR"),
    UPPER_ENGINE("UPPER ENGINE"),
    MEDBAY("MEDBAY"),
    CORRIDOR("CORRIDOR");

    private final String displayName;

    RoomId(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}