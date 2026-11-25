package starfall.entities;

public class Impostor extends Character {

    private double killCooldown = 0.0;
    private double maxKillCooldown = 20.0;
    private double killRange = 90.0;
    private boolean insideVent = false;
    private String currentVentId = null;

    public Impostor(String id, String name, double x, double y, PlayerColor color) {
        super(id, name, x, y, color, PlayerRole.IMPOSTOR);
    }

    public void updateCooldowns(double dt) {
        if (killCooldown > 0.0 && !insideVent) {
            killCooldown = Math.max(0.0, killCooldown - dt);
        }
    }

    public boolean canKill() {
        return alive && !insideVent && killCooldown <= 0.0;
    }

    public void triggerKillCooldown() {
        this.killCooldown = maxKillCooldown;
    }

    public double getKillCooldown() { return killCooldown; }
    public void setKillCooldown(double killCooldown) { this.killCooldown = Math.max(0.0, killCooldown); }

    public double getMaxKillCooldown() { return maxKillCooldown; }
    public void setMaxKillCooldown(double maxKillCooldown) { this.maxKillCooldown = maxKillCooldown; }

    public double getKillRange() { return killRange; }
    public void setKillRange(double killRange) { this.killRange = killRange; }

    public boolean isInsideVent() { return insideVent; }
    public void setInsideVent(boolean insideVent) { this.insideVent = insideVent; }

    public String getCurrentVentId() { return currentVentId; }
    public void setCurrentVentId(String currentVentId) { this.currentVentId = currentVentId; }
}