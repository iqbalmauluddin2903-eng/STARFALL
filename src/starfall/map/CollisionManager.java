package starfall.map;

import java.awt.*;
import java.awt.geom.Area;
import java.util.List;

public class CollisionManager {

    private final Area walkableArea = new Area();
    private final List<Door> doors;

    public CollisionManager(List<Room> rooms, List<Door> doors) {
        this.doors = doors;
        rebuildGeometry(rooms);
    }

    public void rebuildGeometry(List<Room> rooms) {
        walkableArea.reset();
        for (Room room : rooms) {
            walkableArea.add(new Area(room.getBounds()));
        }
        for (Room room : rooms) {
            for (Wall prop : room.getProps()) {
                if (prop.isSolid()) {
                    walkableArea.subtract(new Area(prop.getBounds()));
                }
            }
        }
    }

    public boolean canMoveTo(double x, double y, double radius) {
        // Sample 8 points around the character's collision circle for smooth wall sliding
        double diag = radius * 0.7071;
        double[][] checkPoints = {
            {x - radius, y}, {x + radius, y},
            {x, y - radius}, {x, y + radius},
            {x - diag, y - diag}, {x + diag, y - diag},
            {x - diag, y + diag}, {x + diag, y + diag}
        };

        for (double[] pt : checkPoints) {
            if (!walkableArea.contains(pt[0], pt[1])) {
                return false;
            }
        }

        // Check closed doors
        Rectangle hitbox = new Rectangle(
                (int) (x - radius), (int) (y - radius),
                (int) (radius * 2), (int) (radius * 2)
        );
        for (Door door : doors) {
            if (door.isClosed() && door.getBounds().intersects(hitbox)) {
                return false;
            }
        }
        return true;
    }

    public Area getWalkableArea() {
        return walkableArea;
    }
}