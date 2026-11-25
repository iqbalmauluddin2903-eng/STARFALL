package starfall.ai;

import starfall.core.GameEngine;
import starfall.entities.Character;
import starfall.entities.Impostor;
import starfall.entities.Player;
import starfall.entities.PlayerRole;
import starfall.gameplay.*;
import starfall.map.GameMap;
import starfall.map.InteractiveObject;
import starfall.map.Vent;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class ImpostorAI extends AIController {

    private Character huntTarget = null;
    private double sabotageDecisionTimer = 8.0;
    private double ventStayTimer = 0.0;

    public ImpostorAI(Player player, AINavigation navigation) {
        super(player, navigation);
        this.stateTimer = 0.8 + rng.nextDouble() * 1.2;
    }

    @Override
    protected void updateBehavior(double dt, GameEngine engine) {
        if (!(player.getCharacter() instanceof Impostor imp)) return;

        GameMap map = engine.getGameMap();
        KillManager killMgr = engine.getKillManager();
        VentManager ventMgr = engine.getVentManager();
        SabotageManager sabMgr = engine.getSabotageManager();
        List<Player> players = engine.getPlayers();

        // 1. Strategic Sabotage Decision
        sabotageDecisionTimer -= dt;
        if (sabotageDecisionTimer <= 0.0 && !imp.isInsideVent()) {
            sabotageDecisionTimer = 14.0 + rng.nextDouble() * 10.0;
            if (sabMgr.getActiveSabotage() == SabotageType.NONE && sabMgr.getSabotageCooldown() <= 0.0) {
                SabotageType[] choices = {
                    SabotageType.LIGHTS_SABOTAGE,
                    SabotageType.REACTOR_MELTDOWN,
                    SabotageType.OXYGEN_DEPLETION,
                    SabotageType.COMMUNICATIONS_SABOTAGE,
                    SabotageType.DOOR_LOCKDOWN
                };
                sabMgr.triggerSabotage(choices[rng.nextInt(choices.length)], map);
            }
        }

        // 2. State Machine Execution
        switch (state) {
            case VENTING -> {
                imp.setVelocity(0, 0);
                ventStayTimer -= dt;
                if (ventStayTimer <= 0.0) {
                    if (imp.isInsideVent()) {
                        ventMgr.cycleVent(imp, map, 1);
                        ventMgr.toggleVent(imp, map);
                    }
                    Point fleePt = navigation.getRandomPatrolPoint(rng, imp.getCurrentRoom());
                    navigateTo(fleePt.x, fleePt.y);
                    state = AIState.FLEEING;
                }
            }
            case FLEEING -> {
                if (followPath(dt, 1.12)) {
                    state = AIState.IDLE;
                    stateTimer = 1.0 + rng.nextDouble() * 1.5;
                }
            }
            case HUNTING_TARGET -> {
                if (huntTarget == null || !huntTarget.isAlive() || !imp.canKill()) {
                    huntTarget = null;
                    state = AIState.IDLE;
                    return;
                }

                // Abort hunt if target moves into a crowd of witnesses
                int witnesses = countWitnessesAround(huntTarget, players, 410.0);
                if (witnesses > 0 && !isWinningKill(players)) {
                    huntTarget = null;
                    state = AIState.FAKE_TASKING;
                    stateTimer = 2.0;
                    return;
                }

                // Update pursuit path periodically
                if (path.isEmpty() || rng.nextDouble() < 0.08) {
                    navigateTo(huntTarget.getX(), huntTarget.getY());
                }
                followPath(dt, 1.08);

                if (imp.distanceTo(huntTarget.getX(), huntTarget.getY()) <= imp.getKillRange() * 0.92) {
                    Character victim = huntTarget;
                    if (killMgr.executeKill(imp, victim)) {
                        engine.notifyAllAIsOfKill(imp, victim);
                        huntTarget = null;

                        // Escape via nearest vent if available, otherwise flee to another room
                        Vent nearbyVent = map.findNearestVent(imp.getX(), imp.getY(), 150.0);
                        if (nearbyVent != null && ventMgr.toggleVent(imp, map)) {
                            state = AIState.VENTING;
                            ventStayTimer = 0.65;
                        } else {
                            Point escape = navigation.getRandomPatrolPoint(rng, imp.getCurrentRoom());
                            navigateTo(escape.x, escape.y);
                            state = AIState.FLEEING;
                        }
                    }
                }
            }
            case FAKE_TASKING -> {
                if (followPath(dt, 0.95)) {
                    imp.setVelocity(0, 0);
                    stateTimer -= dt;
                    if (stateTimer <= 0.0) {
                        state = AIState.IDLE;
                        stateTimer = 0.8 + rng.nextDouble() * 1.2;
                    }
                }
            }
            case WANDERING -> {
                // Opportunistically switch to hunting if an isolated victim enters sight range
                if (imp.canKill()) {
                    Character isolated = findIsolatedVictim(imp, players);
                    if (isolated != null) {
                        huntTarget = isolated;
                        navigateTo(isolated.getX(), isolated.getY());
                        state = AIState.HUNTING_TARGET;
                        return;
                    }
                }
                if (followPath(dt, 1.0)) {
                    state = AIState.IDLE;
                    stateTimer = 0.7 + rng.nextDouble() * 1.2;
                }
            }
            case IDLE -> {
                imp.setVelocity(0, 0);
                stateTimer -= dt;
                if (stateTimer <= 0.0) {
                    if (imp.canKill()) {
                        Character isolated = findIsolatedVictim(imp, players);
                        if (isolated != null) {
                            huntTarget = isolated;
                            navigateTo(isolated.getX(), isolated.getY());
                            state = AIState.HUNTING_TARGET;
                            return;
                        }
                    }

                    // 45% fake task to blend in, 55% patrol rooms looking for isolated targets
                    if (rng.nextInt(100) < 45 && !map.getAllInteractables().isEmpty()) {
                        List<InteractiveObject> list = map.getAllInteractables();
                        InteractiveObject fakeObj = list.get(rng.nextInt(list.size()));
                        Point pt = navigation.getSafeInteractPoint(fakeObj, map);
                        navigateTo(pt.x, pt.y);
                        state = AIState.FAKE_TASKING;
                        stateTimer = 2.2 + rng.nextDouble() * 1.8;
                    } else {
                        Point patrol = navigation.getRandomPatrolPoint(rng, imp.getCurrentRoom());
                        navigateTo(patrol.x, patrol.y);
                        state = AIState.WANDERING;
                    }
                }
            }
            default -> state = AIState.IDLE;
        }
    }

    private Character findIsolatedVictim(Impostor imp, List<Player> players) {
        Character best = null;
        double bestDist = 520.0;

        for (Player p : players) {
            Character c = p.getCharacter();
            if (c == imp || !c.isAlive() || c.getRole() == PlayerRole.IMPOSTOR || c.getRole() == PlayerRole.GHOST) {
                continue;
            }
            double d = imp.distanceTo(c.getX(), c.getY());
            if (d <= bestDist) {
                int witnesses = countWitnessesAround(c, players, 410.0);
                if (witnesses == 0 || isWinningKill(players)) {
                    bestDist = d;
                    best = c;
                }
            }
        }
        return best;
    }

    private int countWitnessesAround(Character target, List<Player> players, double radius) {
        Character self = player.getCharacter();
        int witnesses = 0;
        for (Player p : players) {
            Character c = p.getCharacter();
            if (c == self || c == target || !c.isAlive() || c.getRole() == PlayerRole.IMPOSTOR) continue;
            if (c.distanceTo(target.getX(), target.getY()) <= radius) {
                witnesses++;
            }
        }
        return witnesses;
    }

    private boolean isWinningKill(List<Player> players) {
        int aliveCrew = 0;
        int aliveImp = 0;
        for (Player p : players) {
            Character c = p.getCharacter();
            if (!c.isAlive()) continue;
            if (c.getRole() == PlayerRole.IMPOSTOR) aliveImp++;
            else if (c.getRole() == PlayerRole.CREWMATE) aliveCrew++;
        }
        return (aliveCrew - 1) <= aliveImp;
    }

    @Override
    public String generateMeetingStatement(MeetingManager meetingMgr, List<Player> allPlayers) {
        // Fabricate a believable alibi or subtly cast doubt on a living Crewmate
        List<Player> crewTargets = new ArrayList<>();
        for (Player p : allPlayers) {
            Character c = p.getCharacter();
            if (c.isAlive() && c.getRole() == PlayerRole.CREWMATE) {
                crewTargets.add(p);
            }
        }

        if (!crewTargets.isEmpty() && rng.nextInt(100) < 45) {
            Player scapegoat = crewTargets.get(rng.nextInt(crewTargets.size()));
            return "I saw " + scapegoat.getCharacter().getName() + " acting super suspicious near the vents!";
        }

        String[] alibis = {
            "I was calibrating systems in " + player.getCharacter().getCurrentRoom().getDisplayName() + ".",
            "Not me, I've been doing my tasks the whole round.",
            "Who was closest to the body? Let's think carefully."
        };
        return alibis[rng.nextInt(alibis.length)];
    }

    @Override
    public String chooseVoteTarget(List<Player> allPlayers, VotingManager votingMgr) {
        List<Player> crewTargets = new ArrayList<>();
        for (Player p : allPlayers) {
            Character c = p.getCharacter();
            if (c.isAlive() && c.getRole() == PlayerRole.CREWMATE) {
                crewTargets.add(p);
            }
        }

        if (crewTargets.isEmpty() || rng.nextInt(100) < 35) {
            return VotingManager.SKIP_VOTE;
        }
        return crewTargets.get(rng.nextInt(crewTargets.size())).getPlayerId();
    }
}