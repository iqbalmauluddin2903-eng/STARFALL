package starfall.ai;

import starfall.map.CollisionManager;
import starfall.map.GameMap;
import starfall.map.InteractiveObject;
import starfall.map.RoomId;

import java.awt.Point;
import java.util.*;

public class AINavigation {

    private static class NavNode {
        final String id;
        final RoomId roomId;
        final int x;
        final int y;
        final List<String> neighbors = new ArrayList<>();

        NavNode(String id, RoomId roomId, int x, int y, String... links) {
            this.id = id;
            this.roomId = roomId;
            this.x = x;
            this.y = y;
            Collections.addAll(this.neighbors, links);
        }
    }

    private final Map<String, NavNode> nodes = new LinkedHashMap<>();
    private final List<NavNode> roomDestinationNodes = new ArrayList<>();

    public AINavigation() {
        buildNavigationGraph();
    }

    private void buildNavigationGraph() {
        // Cafeteria & threshold nodes (avoiding central cafeteria table)
        addNode("cafe_south", RoomId.CAFETERIA, 1630, 485, "cafe_west", "cafe_east", "corr_central_top");
        addNode("cafe_west", RoomId.CAFETERIA, 1440, 325, "cafe_south", "corr_top_left");
        addNode("cafe_east", RoomId.CAFETERIA, 1820, 325, "cafe_south", "weapons_center");

        // Top-Left & MedBay & Upper Engine
        addNode("corr_top_left", RoomId.CORRIDOR, 1100, 325, "cafe_west", "upper_eng", "medbay_door");
        addNode("medbay_door", RoomId.CORRIDOR, 1100, 470, "corr_top_left", "medbay_center");
        addNode("medbay_center", RoomId.MEDBAY, 1100, 595, "medbay_door");
        addNode("upper_eng", RoomId.UPPER_ENGINE, 605, 325, "corr_top_left", "spine_left_mid");

        // Left Spine: Reactor, Security, Lower Engine, Electrical
        addNode("spine_left_mid", RoomId.CORRIDOR, 605, 880, "upper_eng", "reactor_center", "security_center", "lower_eng");
        addNode("reactor_center", RoomId.REACTOR, 340, 900, "spine_left_mid");
        addNode("security_center", RoomId.SECURITY, 845, 870, "spine_left_mid");
        addNode("lower_eng", RoomId.LOWER_ENGINE, 605, 1545, "spine_left_mid", "corr_bot_left");
        addNode("corr_bot_left", RoomId.CORRIDOR, 1050, 1545, "lower_eng", "electrical_center", "storage_sw");
        addNode("electrical_center", RoomId.ELECTRICAL, 1050, 1320, "corr_bot_left");

        // Central Spine & Admin
        addNode("corr_central_top", RoomId.CORRIDOR, 1630, 680, "cafe_south", "corr_central_mid");
        addNode("corr_central_mid", RoomId.CORRIDOR, 1630, 895, "corr_central_top", "admin_center", "storage_north");
        addNode("admin_center", RoomId.ADMIN, 1900, 895, "corr_central_mid");

        // Storage (routed around crates)
        addNode("storage_north", RoomId.STORAGE, 1630, 1250, "corr_central_mid", "storage_nw", "storage_east");
        addNode("storage_nw", RoomId.STORAGE, 1380, 1250, "storage_north", "storage_sw");
        addNode("storage_sw", RoomId.STORAGE, 1380, 1545, "storage_nw", "corr_bot_left");
        addNode("storage_east", RoomId.STORAGE, 1720, 1390, "storage_north", "corr_bot_right");

        // Bottom-Right & Communications & Shields
        addNode("corr_bot_right", RoomId.CORRIDOR, 1975, 1425, "storage_east", "comms_center", "shields_center");
        addNode("comms_center", RoomId.COMMUNICATIONS, 1975, 1650, "corr_bot_right");
        addNode("shields_center", RoomId.SHIELDS, 2425, 1425, "corr_bot_right", "spine_right_low");

        // Right Spine: Weapons, O2, Navigation
        addNode("weapons_center", RoomId.WEAPONS, 2360, 325, "cafe_east", "spine_right_high");
        addNode("spine_right_high", RoomId.CORRIDOR, 2425, 715, "weapons_center", "o2_center", "spine_right_mid");
        addNode("o2_center", RoomId.O2, 2295, 715, "spine_right_high");
        addNode("spine_right_mid", RoomId.CORRIDOR, 2425, 840, "spine_right_high", "nav_center", "spine_right_low");
        addNode("nav_center", RoomId.NAVIGATION, 2800, 840, "spine_right_mid");
        addNode("spine_right_low", RoomId.CORRIDOR, 2425, 1150, "spine_right_mid", "shields_center");

        for (NavNode node : nodes.values()) {
            if (node.roomId != RoomId.CORRIDOR) {
                roomDestinationNodes.add(node);
            }
        }
    }

    private void addNode(String id, RoomId roomId, int x, int y, String... links) {
        nodes.put(id, new NavNode(id, roomId, x, y, links));
    }

    public List<Point> findPath(double startX, double startY, double goalX, double goalY) {
        NavNode startNode = findNearestNode(startX, startY);
        NavNode goalNode = findNearestNode(goalX, goalY);

        List<Point> result = new ArrayList<>();
        if (startNode == null || goalNode == null) {
            result.add(new Point((int) goalX, (int) goalY));
            return result;
        }

        if (startNode == goalNode) {
            result.add(new Point((int) goalX, (int) goalY));
            return result;
        }

        // Breadth-First Search for shortest room-to-room path
        Queue<NavNode> queue = new ArrayDeque<>();
        Map<String, String> parentMap = new HashMap<>();
        Set<String> visited = new HashSet<>();

        queue.add(startNode);
        visited.add(startNode.id);

        boolean found = false;
        while (!queue.isEmpty()) {
            NavNode current = queue.poll();
            if (current.id.equals(goalNode.id)) {
                found = true;
                break;
            }
            for (String nextId : current.neighbors) {
                if (!visited.contains(nextId) && nodes.containsKey(nextId)) {
                    visited.add(nextId);
                    parentMap.put(nextId, current.id);
                    queue.add(nodes.get(nextId));
                }
            }
        }

        if (found) {
            LinkedList<Point> chain = new LinkedList<>();
            String currId = goalNode.id;
            while (currId != null) {
                NavNode n = nodes.get(currId);
                chain.addFirst(new Point(n.x, n.y));
                currId = parentMap.get(currId);
            }
            // Skip first waypoint if already right next to it
            if (chain.size() > 1 && Math.hypot(startX - chain.getFirst().x, startY - chain.getFirst().y) < 55.0) {
                chain.removeFirst();
            }
            result.addAll(chain);
        }

        result.add(new Point((int) goalX, (int) goalY));
        return result;
    }

    public Point getSafeInteractPoint(InteractiveObject obj, GameMap map) {
        CollisionManager cm = map.getCollisionManager();
        double ox = obj.getX();
        double oy = obj.getY();

        if (cm.canMoveTo(ox, oy, 18.0)) {
            return new Point((int) ox, (int) oy);
        }

        // Probe offsets within interaction radius to avoid overlapping solid props (e.g., Admin table, Nav console)
        int[][] offsets = {
            {-42, 0}, {42, 0}, {0, -42}, {0, 42},
            {-34, -34}, {34, -34}, {-34, 34}, {34, 34},
            {-55, 0}, {55, 0}, {0, -55}, {0, 55}
        };
        for (int[] off : offsets) {
            double px = ox + off[0];
            double py = oy + off[1];
            if (cm.canMoveTo(px, py, 18.0)) {
                return new Point((int) px, (int) py);
            }
        }
        return getWaypointForRoom(obj.getRoomId());
    }

    public Point getWaypointForRoom(RoomId roomId) {
        for (NavNode n : roomDestinationNodes) {
            if (n.roomId == roomId) {
                return new Point(n.x, n.y);
            }
        }
        return new Point(1630, 485);
    }

    public Point getRandomPatrolPoint(Random rng, RoomId excludeRoom) {
        List<NavNode> candidates = new ArrayList<>();
        for (NavNode n : roomDestinationNodes) {
            if (n.roomId != excludeRoom) {
                candidates.add(n);
            }
        }
        if (candidates.isEmpty()) candidates = roomDestinationNodes;
        NavNode pick = candidates.get(rng.nextInt(candidates.size()));
        return new Point(pick.x + rng.nextInt(36) - 18, pick.y + rng.nextInt(36) - 18);
    }

    private NavNode findNearestNode(double x, double y) {
        NavNode best = null;
        double bestDist = Double.MAX_VALUE;
        for (NavNode n : nodes.values()) {
            double d = Math.hypot(x - n.x, y - n.y);
            if (d < bestDist) {
                bestDist = d;
                best = n;
            }
        }
        return best;
    }
}