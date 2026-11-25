package starfall.entities;

public class Ghost extends Character {

    private final PlayerRole originalRole;

    public Ghost(Character deceased) {
        super(deceased.getId(), deceased.getName(), deceased.getX(), deceased.getY(),
              deceased.getColor(), PlayerRole.GHOST);
        this.originalRole = deceased.getRole();
        this.hat = deceased.getHat();
        this.skin = deceased.getSkin();
        this.pet = deceased.getPet();
        this.taskProgress = deceased.getTaskProgress();
        this.alive = false;
        this.movementState = MovementState.GHOST_FLOATING;
        this.moveSpeed = deceased.getMoveSpeed() * 1.15;
    }

    public PlayerRole getOriginalRole() {
        return originalRole;
    }
}