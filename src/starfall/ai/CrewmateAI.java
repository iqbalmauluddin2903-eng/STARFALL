package starfall.ai;

import java.awt.Point;
import java.util.*;
import starfall.core.GameEngine;
import starfall.entities.Character;
import starfall.entities.Crewmate;
import starfall.entities.Player;
import starfall.gameplay.*;
import starfall.map.GameMap;
import starfall.map.InteractiveObject;
import starfall.map.RoomId;
import starfall.tasks.Task;
import starfall.tasks.TaskRegistry;

public class CrewmateAI extends AIController {

    private final List<Task> personalTasks = new ArrayList<>();
    private Task currentTask = null;
    private Character targetBody = null;
    private double reactionTimer = 0.0;

    public CrewmateAI(Player player, AINavigation navigation) {
        super(player, navigation);
        List<Task> pool = TaskRegistry.createAllShipTasks();
        Collections.shuffle(pool, rng);
        int count = Math.min(4, pool.size());
        for (int i = 0; i < count; i++) {
            personalTasks.add(pool.get(i));
        }
        if (player.getCharacter() instanceof Crewmate crew) {
            crew.setTotalAssignedTasks(count);
        }
        this.stateTimer = 0.5 + rng.nextDouble() * 1.2;
    }

    @Override
    protected void updateBehavior(double dt, GameEngine engine) {
        Character self = player.getCharacter();
        GameMap map = engine.getGameMap();
        SabotageManager sabMgr = engine.getSabotageManager();
        TaskManager taskMgr = engine.getTaskManager();

        if (reactionTimer > 0.0) {
            reactionTimer = Math.max(0.0, reactionTimer - dt);
        }

        // 1. Highest Priority: Spot an unreported dead body in visual range
        if (state != AIState.REPORTING_BODY) {
            Character visibleBody = findVisibleUnreportedBody(engine.getPlayers());
            if (visibleBody != null) {
                targetBody = visibleBody;
                state = AIState.REPORTING_BODY;
                reactionTimer = 0.35 + rng.nextDouble() * 0.4;
                navigateTo(visibleBody.getDeadBodyX(), visibleBody.getDeadBodyY());
            }
        }

        // 2. Second Priority: Respond to lethal ship sabotage
        SabotageType activeSab = sabMgr.getActiveSabotage();
        boolean criticalSab = (activeSab == SabotageType.REACTOR_MELTDOWN || activeSab == SabotageType.OXYGEN_DEPLETION);
        if (criticalSab && state != AIState.REPORTING_BODY && state != AIState.RESPONDING_TO_SABOTAGE) {
            InteractiveObject repairObj = findSabotageRepairObject(map, sabMgr);
            if (repairObj != null) {
                state = AIState.RESPONDING_TO_SABOTAGE;
                Point safePt = navigation.getSafeInteractPoint(repairObj, map);
                navigateTo(safePt.x, safePt.y);
            }
        }

        switch (state) {
            case REPORTING_BODY -> {
                if (targetBody == null || !targetBody.hasUnreportedBody() || targetBody.isBodyReported()) {
                    state = AIState.IDLE;
                    return;
                }
                boolean arrived = followPath(dt, 1.15);
                if ((arrived || self.distanceTo(targetBody.getDeadBodyX(), targetBody.getDeadBodyY()) <= 85.0) && reactionTimer <= 0.0) {
                    engine.triggerAIBodyReport(player, targetBody);
                    targetBody = null;
                    state = AIState.IDLE;
                }
            }
            case RESPONDING_TO_SABOTAGE -> {
                if (!criticalSab) {
                    state = AIState.IDLE;
                    return;
                }
                InteractiveObject repairObj = findSabotageRepairObject(map, sabMgr);
                if (repairObj == null) {
                    state = AIState.IDLE;
                    return;
                }
                boolean arrived = followPath(dt, 1.12);
                if (arrived || self.distanceTo(repairObj.getX(), repairObj.getY()) <= 95.0) {
                    self.setVelocity(0, 0);
                    sabMgr.repairActiveSabotage();
                    state = AIState.IDLE;
                    stateTimer = 0.8;
                }
            }
            case MOVING_TO_TASK -> {
                if (currentTask == null || currentTask.isCompleted()) {
                    state = AIState.IDLE;
                    return;
                }
                if (followPath(dt, 1.0)) {
                    self.setVelocity(0, 0);
                    state = AIState.DOING_TASK;
                    stateTimer = 2.0 + rng.nextDouble() * 1.8;
                }
            }
            case DOING_TASK -> {
                self.setVelocity(0, 0);
                stateTimer -= dt;
                if (stateTimer <= 0.0) {
                    if (currentTask != null && !currentTask.isCompleted()) {
                        taskMgr.recordExternalTaskCompletion(player, currentTask);
                    }
                    currentTask = null;
                    state = AIState.IDLE;
                    stateTimer = 0.6 + rng.nextDouble() * 1.0;
                }
            }
            case WANDERING -> {
                if (followPath(dt, 0.92)) {
                    state = AIState.IDLE;
                    stateTimer = 1.0 + rng.nextDouble() * 1.5;
                }
            }
            case IDLE -> {
                self.setVelocity(0, 0);
                stateTimer -= dt;
                if (stateTimer <= 0.0) {
                    Task nextTask = pickNextIncompleteTask();
                    if (nextTask != null) {
                        currentTask = nextTask;
                        InteractiveObject term = findInteractableById(map, nextTask.getInteractableId());
                        Point dest = (term != null)
                                ? navigation.getSafeInteractPoint(term, map)
                                : navigation.getWaypointForRoom(nextTask.getLocation());
                        navigateTo(dest.x, dest.y);
                        state = AIState.MOVING_TO_TASK;
                    } else {
                        Point patrol = navigation.getRandomPatrolPoint(rng, self.getCurrentRoom());
                        navigateTo(patrol.x, patrol.y);
                        state = AIState.WANDERING;
                    }
                }
            }
            default -> state = AIState.IDLE;
        }
    }

    private Task pickNextIncompleteTask() {
        for (Task t : personalTasks) {
            if (!t.isCompleted()) return t;
        }
        return null;
    }

    private Character findVisibleUnreportedBody(List<Player> players) {
        Character self = player.getCharacter();
        double sight = VISUAL_SIGHT_RANGE * 0.92;
        for (Player p : players) {
            Character c = p.getCharacter();
            if (c.hasUnreportedBody() && !c.isBodyReported()) {
                if (self.distanceTo(c.getDeadBodyX(), c.getDeadBodyY()) <= sight) {
                    return c;
                }
            }
        }
        return null;
    }

    private InteractiveObject findSabotageRepairObject(GameMap map, SabotageManager sabMgr) {
        for (InteractiveObject obj : map.getAllInteractables()) {
            if (sabMgr.canRepairWithInteractable(obj.getId())) {
                return obj;
            }
        }
        return null;
    }

    private InteractiveObject findInteractableById(GameMap map, String id) {
        for (InteractiveObject obj : map.getAllInteractables()) {
            if (obj.getId().equals(id)) return obj;
        }
        return null;
    }

    @Override
    public String generateMeetingStatement(MeetingManager meetingMgr, List<Player> allPlayers) {
        for (String culpritId : witnessedCulpritIds) {
            Player culprit = findPlayerById(allPlayers, culpritId);
            if (culprit != null && culprit.getCharacter().isAlive()) {
                return witnessedCulpritReason != null
                        ? witnessedCulpritReason
                        : "Vote " + culprit.getCharacter().getName() + "! I caught them red-handed!";
            }
        }

        if (!lastSeenRooms.isEmpty()) {
            List<String> seenIds = new ArrayList<>(lastSeenRooms.keySet());
            String pickId = seenIds.get(rng.nextInt(seenIds.size()));
            Player seenPlayer = findPlayerById(allPlayers, pickId);
            if (seenPlayer != null && seenPlayer.getCharacter().isAlive()) {
                RoomId room = lastSeenRooms.get(pickId);
                return "I saw " + seenPlayer.getCharacter().getName() + " near " + room.getDisplayName() + " earlier.";
            }
        }

        String[] standardLines = {
            "I was working on my assigned tasks. Any hard proof?",
            "Let's not vote randomly without evidence—skip if unsure.",
            "Check who hasn't been doing tasks around the ship."
        };
        return standardLines[rng.nextInt(standardLines.length)];
    }

    @Override
    public String chooseVoteTarget(List<Player> allPlayers, VotingManager votingMgr) {
        for (String culpritId : witnessedCulpritIds) {
            Player culprit = findPlayerById(allPlayers, culpritId);
            if (culprit != null && culprit.getCharacter().isAlive()) {
                return culpritId;
            }
        }

        if (rng.nextInt(100) < 75 || lastSeenRooms.isEmpty()) {
            return VotingManager.SKIP_VOTE;
        }

        List<String> suspects = new ArrayList<>();
        for (String id : lastSeenRooms.keySet()) {
            Player p = findPlayerById(allPlayers, id);
            if (p != null && p.getCharacter().isAlive() && p != player) {
                suspects.add(id);
            }
        }
        if (suspects.isEmpty()) return VotingManager.SKIP_VOTE;
        return suspects.get(rng.nextInt(suspects.size()));
    }
}