package starfall.map;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Room {

    private final RoomId id;
    private final Rectangle bounds;
    private final Color floorColor;
    private final Color trimColor;
    private final boolean corridor;

    private final List<Wall> props = new ArrayList<>();
    private final List<InteractiveObject> interactables = new ArrayList<>();
    private final List<RoomId> neighbors = new ArrayList<>();

    public Room(RoomId id, Rectangle bounds, Color floorColor, Color trimColor, boolean corridor) {
        this.id = id;
        this.bounds = bounds;
        this.floorColor = floorColor;
        this.trimColor = trimColor;
        this.corridor = corridor;
    }

    public void addProp(Wall prop) {
        props.add(prop);
    }

    public void addInteractable(InteractiveObject obj) {
        interactables.add(obj);
    }

    public void addNeighbors(RoomId... ids) {
        Collections.addAll(neighbors, ids);
    }

    public boolean contains(double x, double y) {
        return bounds.contains(x, y);
    }

    public RoomId getId() { return id; }
    public String getDisplayName() { return id.getDisplayName(); }
    public Rectangle getBounds() { return bounds; }
    public Color getFloorColor() { return floorColor; }
    public Color getTrimColor() { return trimColor; }
    public boolean isCorridor() { return corridor; }

    public List<Wall> getProps() { return Collections.unmodifiableList(props); }
    public List<InteractiveObject> getInteractables() { return Collections.unmodifiableList(interactables); }
    public List<RoomId> getNeighbors() { return Collections.unmodifiableList(neighbors); }
}