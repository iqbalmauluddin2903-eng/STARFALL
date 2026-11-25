package starfall.entities;

public class Crewmate extends Character {

    private int completedTasks = 0;
    private int totalAssignedTasks = 5;

    public Crewmate(String id, String name, double x, double y, PlayerColor color) {
        super(id, name, x, y, color, PlayerRole.CREWMATE);
    }

    public void incrementCompletedTasks() {
        completedTasks = Math.min(totalAssignedTasks, completedTasks + 1);
        setTaskProgress(totalAssignedTasks > 0 ? (float) completedTasks / totalAssignedTasks : 1.0f);
    }

    public int getCompletedTasks() { return completedTasks; }
    public int getTotalAssignedTasks() { return totalAssignedTasks; }

    public void setTotalAssignedTasks(int totalAssignedTasks) {
        this.totalAssignedTasks = Math.max(1, totalAssignedTasks);
    }
}