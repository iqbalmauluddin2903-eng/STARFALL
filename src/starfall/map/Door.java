package starfall.map;

import java.awt.*;

public class Door {

    private final String doorId;
    private final RoomId roomA;
    private final RoomId roomB;
    private final Rectangle bounds;
    private final boolean horizontal;

    private boolean closed = false;
    private double lockTimer = 0.0;

    public Door(String doorId, RoomId roomA, RoomId roomB, Rectangle bounds, boolean horizontal) {
        this.doorId = doorId;
        this.roomA = roomA;
        this.roomB = roomB;
        this.bounds = bounds;
        this.horizontal = horizontal;
    }

    public void update(double dt) {
        if (closed && lockTimer > 0.0) {
            lockTimer -= dt;
            if (lockTimer <= 0.0) {
                closed = false;
                lockTimer = 0.0;
            }
        }
    }

    public void lockFor(double seconds) {
        this.closed = true;
        this.lockTimer = seconds;
    }

    public void open() {
        this.closed = false;
        this.lockTimer = 0.0;
    }

    public String getDoorId() { return doorId; }
    public RoomId getRoomA() { return roomA; }
    public RoomId getRoomB() { return roomB; }
    public Rectangle getBounds() { return bounds; }
    public boolean isHorizontal() { return horizontal; }
    public boolean isClosed() { return closed; }
    public double getLockTimer() { return lockTimer; }
}