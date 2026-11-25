package starfall.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Vent {

    private final String ventId;
    private final RoomId roomId;
    private final double x;
    private final double y;
    private final List<String> connectedVentIds = new ArrayList<>();

    public Vent(String ventId, RoomId roomId, double x, double y, String... targets) {
        this.ventId = ventId;
        this.roomId = roomId;
        this.x = x;
        this.y = y;
        Collections.addAll(this.connectedVentIds, targets);
    }

    public String getVentId() { return ventId; }
    public RoomId getRoomId() { return roomId; }
    public double getX() { return x; }
    public double getY() { return y; }
    public List<String> getConnectedVentIds() { return Collections.unmodifiableList(connectedVentIds); }
}