package starfall.gameplay;

import starfall.map.RoomId;

public enum SabotageType {
    NONE("None", RoomId.CORRIDOR, 0.0, false),
    REACTOR_MELTDOWN("REACTOR MELTDOWN", RoomId.REACTOR, 30.0, true),
    OXYGEN_DEPLETION("OXYGEN DEPLETION", RoomId.O2, 30.0, true),
    LIGHTS_SABOTAGE("LIGHTS SABOTAGED", RoomId.ELECTRICAL, 0.0, false),
    COMMUNICATIONS_SABOTAGE("COMMS SABOTAGED", RoomId.COMMUNICATIONS, 0.0, false),
    DOOR_LOCKDOWN("DOOR LOCKDOWN", RoomId.CAFETERIA, 10.0, false);

    private final String warningTitle;
    private final RoomId repairRoom;
    private final double durationSeconds;
    private final boolean lethalOnTimeout;

    SabotageType(String warningTitle, RoomId repairRoom, double durationSeconds, boolean lethalOnTimeout) {
        this.warningTitle = warningTitle;
        this.repairRoom = repairRoom;
        this.durationSeconds = durationSeconds;
        this.lethalOnTimeout = lethalOnTimeout;
    }

    public String getWarningTitle() { return warningTitle; }
    public RoomId getRepairRoom() { return repairRoom; }
    public double getDurationSeconds() { return durationSeconds; }
    public boolean isLethalOnTimeout() { return lethalOnTimeout; }
}