package starfall.ai;

import starfall.core.GameEngine;
import starfall.entities.Character;
import starfall.entities.MovementState;
import starfall.entities.Player;
import starfall.entities.PlayerRole;
import starfall.gameplay.MeetingManager;
import starfall.gameplay.VotingManager;
import starfall.map.RoomId;

import java.awt.Point;
import java.util.*;

public abstract class AIController {

    protected static final double VISUAL_SIGHT_RANGE = 400.0;

    protected final Player player;
    protected final AINavigation navigation;
    protected final Random rng = new Random();

    protected AIState state = AIState.IDLE;
    protected final LinkedList<Point> path = new LinkedList<>();
    protected double stateTimer = 0.0;
    protected double stuckTimer = 0.0;
    protected double lastCheckX = 0.0;
    protected double lastCheckY = 0.0;

    // Fair visual memory (only updated for players within sight range)
    protected final Map<String, RoomId> lastSeenRooms = new HashMap<>();
    protected final Set<String> witnessedCulpritIds = new LinkedHashSet<>();
    protected String witnessedCulpritReason = null;

    protected AIController(Player player, AINavigation navigation) {
        this.player = player;
        this.navigation = navigation;
        this.lastCheckX = player.getCharacter().getX();
        this.lastCheckY = player.getCharacter().getY();
    }

    public void onMeetingStarted() {
        path.clear();
        state = AIState.IDLE;
        stateTimer = 0.0;
        stuckTimer = 0.0;
        player.getCharacter().setVelocity(0, 0);
    }

    public void onMeetingEnded() {
        // Clear single-round sighting room cache while retaining witnessed murder/vent proof
        lastSeenRooms.clear();
        path.clear();
        state = AIState.IDLE;
        stateTimer = 0.6 + rng.nextDouble() * 1.2;
    }

    public final void update(double dt, GameEngine engine) {
        Character self = player.getCharacter();
        if (!self.isConnected()) return;

        // Ghosts float around completing remaining tasks quietly
        if (!self.isAlive() || self.getRole() == PlayerRole.GHOST) {
            updateGhostWander(dt);
            return;
        }

        updateVisualPerception(engine.getPlayers());
        updateBehavior(dt, engine);
    }

    private void updateVisualPerception(List<Player> allPlayers) {
        Character self = player.getCharacter();
        if (self.getMovementState() == MovementState.VENTING) return;

        for (Player other : allPlayers) {
            if (other == player) continue;
            Character oc = other.getCharacter();
            if (!oc.isAlive()) continue;

            double dist = self.distanceTo(oc.getX(), oc.getY());
            if (dist <= VISUAL_SIGHT_RANGE) {
                lastSeenRooms.put(other.getPlayerId(), oc.getCurrentRoom());

                // Visually catch an Impostor venting!
                if (oc.getMovementState() == MovementState.VENTING && self.getRole() == PlayerRole.CREWMATE) {
                    witnessedCulpritIds.add(other.getPlayerId());
                    witnessedCulpritReason = "I saw " + oc.getName() + " use a vent in " + oc.getCurrentRoom().getDisplayName() + "!";
                }
            }
        }
    }

    public void notifyKillObserved(Character killer, Character victim, List<Player> allPlayers) {
        Character self = player.getCharacter();
        if (!self.isAlive() || self.getRole() != PlayerRole.CREWMATE) return;
        if (killer.getId().equals(self.getId()) || victim.getId().equals(self.getId())) return;

        if (self.distanceTo(victim.getX(), victim.getY()) <= VISUAL_SIGHT_RANGE) {
            for (Player p : allPlayers) {
                if (p.getCharacter() == killer) {
                    witnessedCulpritIds.add(p.getPlayerId());
                    witnessedCulpritReason = "I literally saw " + killer.getName() + " eliminate "
                            + victim.getName() + " in " + victim.getCurrentRoom().getDisplayName() + "!";
                    break;
                }
            }
        }
    }

    protected void navigateTo(double targetX, double targetY) {
        Character self = player.getCharacter();
        path.clear();
        path.addAll(navigation.findPath(self.getX(), self.getY(), targetX, targetY));
        stuckTimer = 0.0;
        lastCheckX = self.getX();
        lastCheckY = self.getY();
    }

    /**
     * Moves along the active path and returns true when the final destination is reached.
     */
    protected boolean followPath(double dt, double speedMultiplier) {
        Character self = player.getCharacter();
        if (path.isEmpty()) {
            self.setVelocity(0, 0);
            return true;
        }

        Point next = path.peekFirst();
        double dx = next.x - self.getX();
        double dy = next.y - self.getY();
        double dist = Math.hypot(dx, dy);

        if (dist <= 16.0) {
            path.removeFirst();
            if (path.isEmpty()) {
                self.setVelocity(0, 0);
                return true;
            }
            next = path.peekFirst();
            dx = next.x - self.getX();
            dy = next.y - self.getY();
            dist = Math.hypot(dx, dy);
        }

        double speed = self.getMoveSpeed() * speedMultiplier;
        self.setVelocity((dx / Math.max(0.001, dist)) * speed, (dy / Math.max(0.001, dist)) * speed);

        // Stuck recovery check (e.g., locked door or sharp corner)
        stuckTimer += dt;
        if (stuckTimer >= 1.1) {
            double moved = Math.hypot(self.getX() - lastCheckX, self.getY() - lastCheckY);
            lastCheckX = self.getX();
            lastCheckY = self.getY();
            stuckTimer = 0.0;
            if (moved < 18.0) {
                path.clear();
                self.setVelocity(0, 0);
                return true;
            }
        }
        return false;
    }

    private void updateGhostWander(double dt) {
        if (path.isEmpty()) {
            Point p = navigation.getRandomPatrolPoint(rng, player.getCharacter().getCurrentRoom());
            path.add(p);
        }
        followPath(dt, 0.85);
    }

    protected Player findPlayerById(List<Player> players, String id) {
        for (Player p : players) {
            if (p.getPlayerId().equals(id)) return p;
        }
        return null;
    }

    protected abstract void updateBehavior(double dt, GameEngine engine);

    public abstract String generateMeetingStatement(MeetingManager meetingMgr, List<Player> allPlayers);

    public abstract String chooseVoteTarget(List<Player> allPlayers, VotingManager votingMgr);

    public Player getPlayer() { return player; }
    public AIState getState() { return state; }
}