package starfall.map;

import java.awt.*;
import java.util.*;
import java.util.List;

public class GameMap {

    public static final int WORLD_WIDTH = 3200;
    public static final int WORLD_HEIGHT = 2000;

    private final List<Room> rooms = new ArrayList<>();
    private final List<Door> doors = new ArrayList<>();
    private final List<Vent> vents = new ArrayList<>();
    private final List<InteractiveObject> allInteractables = new ArrayList<>();
    private final Map<RoomId, Point> roomWaypoints = new EnumMap<>(RoomId.class);
    private final CollisionManager collisionManager;

    public GameMap() {
        buildSpaceshipLayout();
        this.collisionManager = new CollisionManager(rooms, doors);
    }

    private void buildSpaceshipLayout() {
        Color stdFloor = new Color(112, 122, 138);
        Color corrFloor = new Color(84, 92, 106);
        Color techFloor = new Color(96, 110, 130);
        Color bioFloor = new Color(92, 126, 122);
        Color cargoFloor = new Color(124, 114, 100);
        Color trimSteel = new Color(52, 60, 74);

        // 1. CAFETERIA (Top Center)
        Room cafeteria = new Room(RoomId.CAFETERIA, new Rectangle(1350, 150, 560, 420), stdFloor, trimSteel, false);
        cafeteria.addNeighbors(RoomId.UPPER_ENGINE, RoomId.WEAPONS, RoomId.STORAGE, RoomId.ADMIN);
        cafeteria.addProp(new Wall(new Rectangle(1570, 300, 120, 120), Wall.PropStyle.CAFETERIA_TABLE, true, new Color(150, 160, 175)));
        addInteractive(cafeteria, new InteractiveObject("btn_emergency", "Emergency Meeting Button", RoomId.CAFETERIA, 1630, 360, 105, InteractiveObject.InteractionType.EMERGENCY_BUTTON));
        addInteractive(cafeteria, new InteractiveObject("task_trash_cafe", "Empty Garbage", RoomId.CAFETERIA, 1850, 210, 80, InteractiveObject.InteractionType.TASK_TERMINAL));
        rooms.add(cafeteria);
        roomWaypoints.put(RoomId.CAFETERIA, new Point(1630, 480));

        // 2. WEAPONS (Top Right)
        Room weapons = new Room(RoomId.WEAPONS, new Rectangle(2220, 180, 360, 300), techFloor, trimSteel, false);
        weapons.addNeighbors(RoomId.CAFETERIA, RoomId.O2, RoomId.NAVIGATION, RoomId.SHIELDS);
        weapons.addProp(new Wall(new Rectangle(2440, 280, 80, 90), Wall.PropStyle.WEAPONS_CHAIR, true, new Color(65, 85, 115)));
        addInteractive(weapons, new InteractiveObject("task_asteroids", "Clear Asteroids", RoomId.WEAPONS, 2410, 325, 85, InteractiveObject.InteractionType.TASK_TERMINAL));
        rooms.add(weapons);
        roomWaypoints.put(RoomId.WEAPONS, new Point(2360, 330));

        // 3. O2 (Upper-Mid Right)
        Room o2 = new Room(RoomId.O2, new Rectangle(2160, 590, 220, 250), bioFloor, trimSteel, false);
        o2.addNeighbors(RoomId.WEAPONS, RoomId.NAVIGATION, RoomId.SHIELDS);
        o2.addProp(new Wall(new Rectangle(2190, 620, 75, 70), Wall.PropStyle.O2_FILTER, true, new Color(75, 150, 130)));
        addInteractive(o2, new InteractiveObject("task_o2_filter", "Clean O2 Filter", RoomId.O2, 2240, 720, 80, InteractiveObject.InteractionType.TASK_TERMINAL));
        addInteractive(o2, new InteractiveObject("sab_o2", "O2 Oxygen Console", RoomId.O2, 2310, 640, 85, InteractiveObject.InteractionType.SABOTAGE_FIX));
        rooms.add(o2);
        roomWaypoints.put(RoomId.O2, new Point(2270, 740));

        // 4. NAVIGATION (Far Right)
        Room navigation = new Room(RoomId.NAVIGATION, new Rectangle(2660, 680, 340, 320), techFloor, trimSteel, false);
        navigation.addNeighbors(RoomId.WEAPONS, RoomId.O2, RoomId.SHIELDS);
        navigation.addProp(new Wall(new Rectangle(2910, 760, 60, 160), Wall.PropStyle.NAVIGATION_CONSOLE, true, new Color(60, 110, 165)));
        addInteractive(navigation, new InteractiveObject("task_nav_steer", "Stabilize Steering", RoomId.NAVIGATION, 2875, 840, 85, InteractiveObject.InteractionType.TASK_TERMINAL));
        rooms.add(navigation);
        roomWaypoints.put(RoomId.NAVIGATION, new Point(2800, 840));

        // 5. SHIELDS (Bottom Right)
        Room shields = new Room(RoomId.SHIELDS, new Rectangle(2220, 1200, 360, 300), techFloor, trimSteel, false);
        shields.addNeighbors(RoomId.NAVIGATION, RoomId.O2, RoomId.COMMUNICATIONS, RoomId.STORAGE);
        shields.addProp(new Wall(new Rectangle(2460, 1310, 80, 110), Wall.PropStyle.SHIELDS_ARRAY, true, new Color(210, 175, 55)));
        addInteractive(shields, new InteractiveObject("task_shields", "Prime Shields", RoomId.SHIELDS, 2430, 1365, 85, InteractiveObject.InteractionType.TASK_TERMINAL));
        rooms.add(shields);
        roomWaypoints.put(RoomId.SHIELDS, new Point(2380, 1350));

        // 6. COMMUNICATIONS (Bottom Mid-Right)
        Room comms = new Room(RoomId.COMMUNICATIONS, new Rectangle(1820, 1560, 320, 250), techFloor, trimSteel, false);
        comms.addNeighbors(RoomId.SHIELDS, RoomId.STORAGE);
        comms.addProp(new Wall(new Rectangle(1920, 1720, 130, 60), Wall.PropStyle.COMMS_TOWER, true, new Color(80, 95, 120)));
        addInteractive(comms, new InteractiveObject("task_comms_dl", "Download Data", RoomId.COMMUNICATIONS, 1985, 1690, 80, InteractiveObject.InteractionType.TASK_TERMINAL));
        addInteractive(comms, new InteractiveObject("sab_comms", "Comms Relay Reset", RoomId.COMMUNICATIONS, 1880, 1630, 85, InteractiveObject.InteractionType.SABOTAGE_FIX));
        rooms.add(comms);
        roomWaypoints.put(RoomId.COMMUNICATIONS, new Point(1980, 1660));

        // 7. STORAGE (Bottom Center)
        Room storage = new Room(RoomId.STORAGE, new Rectangle(1320, 1180, 450, 460), cargoFloor, trimSteel, false);
        storage.addNeighbors(RoomId.CAFETERIA, RoomId.ADMIN, RoomId.COMMUNICATIONS, RoomId.SHIELDS, RoomId.ELECTRICAL, RoomId.LOWER_ENGINE);
        storage.addProp(new Wall(new Rectangle(1430, 1320, 95, 95), Wall.PropStyle.STORAGE_CRATE, true, new Color(155, 118, 72)));
        storage.addProp(new Wall(new Rectangle(1590, 1440, 85, 85), Wall.PropStyle.STORAGE_CRATE, true, new Color(142, 108, 65)));
        addInteractive(storage, new InteractiveObject("task_fuel", "Fuel Engines", RoomId.STORAGE, 1480, 1450, 90, InteractiveObject.InteractionType.TASK_TERMINAL));
        rooms.add(storage);
        roomWaypoints.put(RoomId.STORAGE, new Point(1550, 1340));

        // 8. ADMIN (Mid Center-Right)
        Room admin = new Room(RoomId.ADMIN, new Rectangle(1840, 760, 320, 270), stdFloor, trimSteel, false);
        admin.addNeighbors(RoomId.CAFETERIA, RoomId.STORAGE);
        admin.addProp(new Wall(new Rectangle(1940, 850, 140, 80), Wall.PropStyle.ADMIN_TABLE, true, new Color(55, 135, 95)));
        addInteractive(admin, new InteractiveObject("admin_map", "Admin Map Table", RoomId.ADMIN, 2010, 890, 90, InteractiveObject.InteractionType.ADMIN_MAP));
        addInteractive(admin, new InteractiveObject("task_swipe_card", "Swipe Card", RoomId.ADMIN, 2095, 890, 85, InteractiveObject.InteractionType.TASK_TERMINAL));
        addInteractive(admin, new InteractiveObject("task_upload_admin", "Upload Data", RoomId.ADMIN, 1905, 815, 80, InteractiveObject.InteractionType.TASK_TERMINAL));
        rooms.add(admin);
        roomWaypoints.put(RoomId.ADMIN, new Point(1930, 895));

        // 9. ELECTRICAL (Bottom Mid-Left)
        Room electrical = new Room(RoomId.ELECTRICAL, new Rectangle(880, 1140, 340, 300), techFloor, trimSteel, false);
        electrical.addNeighbors(RoomId.STORAGE, RoomId.LOWER_ENGINE);
        electrical.addProp(new Wall(new Rectangle(920, 1180, 150, 55), Wall.PropStyle.ELECTRICAL_RACK, true, new Color(70, 78, 92)));
        addInteractive(electrical, new InteractiveObject("task_wiring", "Fix Wiring", RoomId.ELECTRICAL, 995, 1265, 80, InteractiveObject.InteractionType.TASK_TERMINAL));
        addInteractive(electrical, new InteractiveObject("sab_lights", "Electrical Breaker", RoomId.ELECTRICAL, 1135, 1210, 85, InteractiveObject.InteractionType.SABOTAGE_FIX));
        rooms.add(electrical);
        roomWaypoints.put(RoomId.ELECTRICAL, new Point(1050, 1320));

        // 10. LOWER ENGINE (Bottom Left)
        Room lowerEngine = new Room(RoomId.LOWER_ENGINE, new Rectangle(440, 1320, 330, 320), techFloor, trimSteel, false);
        lowerEngine.addNeighbors(RoomId.ELECTRICAL, RoomId.STORAGE, RoomId.SECURITY, RoomId.REACTOR, RoomId.UPPER_ENGINE);
        lowerEngine.addProp(new Wall(new Rectangle(480, 1430, 95, 110), Wall.PropStyle.ENGINE_TURBINE, true, new Color(95, 115, 140)));
        addInteractive(lowerEngine, new InteractiveObject("task_align_lower", "Align Engine Output", RoomId.LOWER_ENGINE, 600, 1485, 85, InteractiveObject.InteractionType.TASK_TERMINAL));
        rooms.add(lowerEngine);
        roomWaypoints.put(RoomId.LOWER_ENGINE, new Point(620, 1480));

        // 11. SECURITY (Mid Left)
        Room security = new Room(RoomId.SECURITY, new Rectangle(760, 740, 280, 260), techFloor, trimSteel, false);
        security.addNeighbors(RoomId.UPPER_ENGINE, RoomId.REACTOR, RoomId.LOWER_ENGINE);
        security.addProp(new Wall(new Rectangle(890, 770, 120, 55), Wall.PropStyle.SECURITY_DESK, true, new Color(55, 75, 105)));
        addInteractive(security, new InteractiveObject("sec_cams", "Security Surveillance", RoomId.SECURITY, 950, 850, 80, InteractiveObject.InteractionType.SECURITY_CAMERAS));
        rooms.add(security);
        roomWaypoints.put(RoomId.SECURITY, new Point(880, 880));

        // 12. REACTOR (Far Left)
        Room reactor = new Room(RoomId.REACTOR, new Rectangle(160, 720, 300, 360), techFloor, trimSteel, false);
        reactor.addNeighbors(RoomId.UPPER_ENGINE, RoomId.SECURITY, RoomId.LOWER_ENGINE);
        reactor.addProp(new Wall(new Rectangle(200, 830, 90, 140), Wall.PropStyle.REACTOR_CORE, true, new Color(65, 185, 235)));
        addInteractive(reactor, new InteractiveObject("task_reactor", "Start Reactor", RoomId.REACTOR, 320, 900, 85, InteractiveObject.InteractionType.TASK_TERMINAL));
        addInteractive(reactor, new InteractiveObject("sab_reactor", "Reactor Stabilizer", RoomId.REACTOR, 320, 790, 90, InteractiveObject.InteractionType.SABOTAGE_FIX));
        rooms.add(reactor);
        roomWaypoints.put(RoomId.REACTOR, new Point(340, 900));

        // 13. UPPER ENGINE (Top Left)
        Room upperEngine = new Room(RoomId.UPPER_ENGINE, new Rectangle(440, 180, 330, 310), techFloor, trimSteel, false);
        upperEngine.addNeighbors(RoomId.CAFETERIA, RoomId.MEDBAY, RoomId.REACTOR, RoomId.SECURITY, RoomId.LOWER_ENGINE);
        upperEngine.addProp(new Wall(new Rectangle(480, 270, 95, 110), Wall.PropStyle.ENGINE_TURBINE, true, new Color(95, 115, 140)));
        addInteractive(upperEngine, new InteractiveObject("task_manifolds", "Unlock Manifolds", RoomId.UPPER_ENGINE, 600, 325, 85, InteractiveObject.InteractionType.TASK_TERMINAL));
        rooms.add(upperEngine);
        roomWaypoints.put(RoomId.UPPER_ENGINE, new Point(620, 330));

        // 14. MEDBAY (Top Mid-Left)
        Room medbay = new Room(RoomId.MEDBAY, new Rectangle(940, 520, 320, 300), bioFloor, trimSteel, false);
        medbay.addNeighbors(RoomId.UPPER_ENGINE, RoomId.CAFETERIA);
        medbay.addProp(new Wall(new Rectangle(1055, 635, 90, 90), Wall.PropStyle.MEDBAY_SCANNER, false, new Color(45, 225, 165)));
        addInteractive(medbay, new InteractiveObject("task_med_scan", "Submit Scan", RoomId.MEDBAY, 1100, 680, 80, InteractiveObject.InteractionType.TASK_TERMINAL));
        rooms.add(medbay);
        roomWaypoints.put(RoomId.MEDBAY, new Point(1100, 620));

        // CORRIDORS
        addCorridor(760, 260, 600, 130, corrFloor, trimSteel);
        addCorridor(1035, 380, 130, 150, corrFloor, trimSteel);
        addCorridor(1900, 260, 330, 130, corrFloor, trimSteel);
        addCorridor(1565, 560, 130, 630, corrFloor, trimSteel);
        addCorridor(1685, 830, 165, 130, corrFloor, trimSteel);
        addCorridor(540, 480, 130, 850, corrFloor, trimSteel);
        addCorridor(450, 835, 100, 130, corrFloor, trimSteel);
        addCorridor(660, 805, 110, 130, corrFloor, trimSteel);
        addCorridor(760, 1480, 570, 130, corrFloor, trimSteel);
        addCorridor(985, 1430, 130, 60, corrFloor, trimSteel);
        addCorridor(2360, 470, 130, 740, corrFloor, trimSteel);
        addCorridor(2370, 650, 120, 130, corrFloor, trimSteel);
        addCorridor(2480, 775, 190, 130, corrFloor, trimSteel);
        addCorridor(1760, 1360, 470, 130, corrFloor, trimSteel);
        addCorridor(1910, 1480, 130, 90, corrFloor, trimSteel);

        // DOORS
        doors.add(new Door("door_cafe_west", RoomId.CAFETERIA, RoomId.CORRIDOR, new Rectangle(1345, 260, 16, 130), false));
        doors.add(new Door("door_cafe_east", RoomId.CAFETERIA, RoomId.CORRIDOR, new Rectangle(1900, 260, 16, 130), false));
        doors.add(new Door("door_cafe_south", RoomId.CAFETERIA, RoomId.CORRIDOR, new Rectangle(1565, 560, 130, 16), true));
        doors.add(new Door("door_medbay", RoomId.MEDBAY, RoomId.CORRIDOR, new Rectangle(1035, 515, 130, 16), true));
        doors.add(new Door("door_electrical", RoomId.ELECTRICAL, RoomId.CORRIDOR, new Rectangle(985, 1430, 130, 16), true));
        doors.add(new Door("door_security", RoomId.SECURITY, RoomId.CORRIDOR, new Rectangle(755, 805, 16, 130), false));
        doors.add(new Door("door_upper_eng", RoomId.UPPER_ENGINE, RoomId.CORRIDOR, new Rectangle(760, 260, 16, 130), false));
        doors.add(new Door("door_lower_eng", RoomId.LOWER_ENGINE, RoomId.CORRIDOR, new Rectangle(760, 1480, 16, 130), false));
        doors.add(new Door("door_storage_north", RoomId.STORAGE, RoomId.CORRIDOR, new Rectangle(1565, 1175, 130, 16), true));

        // VENTS
        vents.add(new Vent("vent_medbay", RoomId.MEDBAY, 980, 770, "vent_electrical", "vent_security"));
        vents.add(new Vent("vent_electrical", RoomId.ELECTRICAL, 920, 1390, "vent_security", "vent_medbay"));
        vents.add(new Vent("vent_security", RoomId.SECURITY, 800, 960, "vent_medbay", "vent_electrical"));

        vents.add(new Vent("vent_cafeteria", RoomId.CAFETERIA, 1860, 520, "vent_admin", "vent_weapons"));
        vents.add(new Vent("vent_admin", RoomId.ADMIN, 1880, 990, "vent_weapons", "vent_cafeteria"));
        vents.add(new Vent("vent_weapons", RoomId.WEAPONS, 2530, 220, "vent_cafeteria", "vent_admin"));

        vents.add(new Vent("vent_reactor", RoomId.REACTOR, 220, 760, "vent_upper_eng"));
        vents.add(new Vent("vent_upper_eng", RoomId.UPPER_ENGINE, 480, 220, "vent_reactor"));
        vents.add(new Vent("vent_shields", RoomId.SHIELDS, 2530, 1450, "vent_navigation"));
        vents.add(new Vent("vent_navigation", RoomId.NAVIGATION, 2710, 960, "vent_shields"));
    }

    private void addCorridor(int x, int y, int w, int h, Color floor, Color trim) {
        rooms.add(new Room(RoomId.CORRIDOR, new Rectangle(x, y, w, h), floor, trim, true));
    }

    private void addInteractive(Room room, InteractiveObject obj) {
        room.addInteractable(obj);
        allInteractables.add(obj);
    }

    public void update(double dt) {
        for (Door door : doors) {
            door.update(dt);
        }
    }

    public RoomId getRoomAt(double x, double y) {
        for (Room room : rooms) {
            if (!room.isCorridor() && room.contains(x, y)) {
                return room.getId();
            }
        }
        return RoomId.CORRIDOR;
    }

    public Point getWaypointFor(RoomId roomId) {
        return roomWaypoints.getOrDefault(roomId, new Point(1630, 480));
    }

    public InteractiveObject findNearestInteractable(double x, double y, double maxRange) {
        InteractiveObject best = null;
        double bestDist = maxRange;
        for (InteractiveObject obj : allInteractables) {
            double d = Math.hypot(x - obj.getX(), y - obj.getY());
            if (d <= Math.min(maxRange, obj.getRadius()) && d < bestDist) {
                bestDist = d;
                best = obj;
            }
        }
        return best;
    }

    public Vent findNearestVent(double x, double y, double maxRange) {
        Vent best = null;
        double bestDist = maxRange;
        for (Vent v : vents) {
            double d = Math.hypot(x - v.getX(), y - v.getY());
            if (d <= maxRange && d < bestDist) {
                bestDist = d;
                best = v;
            }
        }
        return best;
    }

    public List<Room> getRooms() { return Collections.unmodifiableList(rooms); }
    public List<Door> getDoors() { return Collections.unmodifiableList(doors); }
    public List<Vent> getVents() { return Collections.unmodifiableList(vents); }
    public List<InteractiveObject> getAllInteractables() { return Collections.unmodifiableList(allInteractables); }
    public CollisionManager getCollisionManager() { return collisionManager; }
}