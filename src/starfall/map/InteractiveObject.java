package starfall.map;

public class InteractiveObject {

    public enum InteractionType {
        EMERGENCY_BUTTON,
        TASK_TERMINAL,
        SECURITY_CAMERAS,
        ADMIN_MAP,
        SABOTAGE_FIX
    }

    private final String id;
    private final String label;
    private final RoomId roomId;
    private final double x;
    private final double y;
    private final double radius;
    private final InteractionType type;
    private boolean completed = false;

    public InteractiveObject(String id, String label, RoomId roomId, double x, double y,
                             double radius, InteractionType type) {
        this.id = id;
        this.label = label;
        this.roomId = roomId;
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.type = type;
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public RoomId getRoomId() { return roomId; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getRadius() { return radius; }
    public InteractionType getType() { return type; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}