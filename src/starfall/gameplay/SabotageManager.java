package starfall.gameplay;

import starfall.map.Door;
import starfall.map.GameMap;
import starfall.utils.Logger;

public class SabotageManager {

    private SabotageType activeSabotage = SabotageType.NONE;
    private double countdown = 0.0;
    private double sabotageCooldown = 0.0;
    private boolean criticalFailureTriggered = false;

    public void reset() {
        activeSabotage = SabotageType.NONE;
        countdown = 0.0;
        sabotageCooldown = 0.0;
        criticalFailureTriggered = false;
    }

    public void syncFromNetwork(SabotageType type, double remainingSeconds) {
        this.activeSabotage = (type != null) ? type : SabotageType.NONE;
        this.countdown = Math.max(0.0, remainingSeconds);
    }

    public boolean triggerSabotage(SabotageType type, GameMap map) {
        if (type == SabotageType.NONE || activeSabotage != SabotageType.NONE || sabotageCooldown > 0.0) {
            return false;
        }

        if (type == SabotageType.DOOR_LOCKDOWN) {
            for (Door door : map.getDoors()) {
                door.lockFor(type.getDurationSeconds());
            }
            sabotageCooldown = 18.0;
            Logger.warn("SABOTAGE TRIGGERED: DOOR LOCKDOWN (10s)");
            return true;
        }

        this.activeSabotage = type;
        this.countdown = type.getDurationSeconds();
        this.sabotageCooldown = 25.0;
        Logger.warn("SABOTAGE TRIGGERED: " + type.getWarningTitle());
        return true;
    }

    public void update(double dt) {
        if (sabotageCooldown > 0.0) {
            sabotageCooldown = Math.max(0.0, sabotageCooldown - dt);
        }

        if (activeSabotage != SabotageType.NONE && activeSabotage.getDurationSeconds() > 0.0) {
            countdown -= dt;
            if (countdown <= 0.0) {
                countdown = 0.0;
                if (activeSabotage.isLethalOnTimeout()) {
                    criticalFailureTriggered = true;
                    Logger.warn("CRITICAL SABOTAGE EXPIRED: " + activeSabotage.getWarningTitle());
                }
                activeSabotage = SabotageType.NONE;
            }
        }
    }

    public void repairActiveSabotage() {
        if (activeSabotage != SabotageType.NONE) {
            Logger.info("Sabotage repaired: " + activeSabotage.getWarningTitle());
            activeSabotage = SabotageType.NONE;
            countdown = 0.0;
        }
    }

    public boolean canRepairWithInteractable(String interactableId) {
        return switch (activeSabotage) {
            case REACTOR_MELTDOWN -> "sab_reactor".equals(interactableId) || "task_reactor".equals(interactableId);
            case OXYGEN_DEPLETION -> "sab_o2".equals(interactableId) || "task_o2_filter".equals(interactableId);
            case LIGHTS_SABOTAGE -> "sab_lights".equals(interactableId) || "task_wiring".equals(interactableId);
            case COMMUNICATIONS_SABOTAGE -> "sab_comms".equals(interactableId) || "task_comms_dl".equals(interactableId);
            default -> false;
        };
    }

    public SabotageType getActiveSabotage() { return activeSabotage; }
    public double getCountdown() { return countdown; }
    public double getSabotageCooldown() { return sabotageCooldown; }
    public boolean isLightsSabotaged() { return activeSabotage == SabotageType.LIGHTS_SABOTAGE; }
    public boolean isCommsSabotaged() { return activeSabotage == SabotageType.COMMUNICATIONS_SABOTAGE; }
    public boolean isCriticalFailureTriggered() { return criticalFailureTriggered; }
}