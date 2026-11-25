package starfall.entities;

import starfall.map.CollisionManager;
import starfall.map.RoomId;

public abstract class Character {

    protected final String id;
    protected String name;
    protected double x;
    protected double y;
    protected double vx;
    protected double vy;

    protected double moveSpeed = 265.0;
    protected double interactionRange = 85.0;
    protected double collisionRadius = 18.0;

    protected boolean facingRight = true;
    protected double walkCycle = 0.0;
    protected double idleTime = 0.0;
    protected double deathTimer = 0.0;

    // Crime scene coordinates for unreported dead body
    protected double deadBodyX;
    protected double deadBodyY;
    protected boolean hasUnreportedBody = false;

    // Companion pet trailing position
    protected double petX;
    protected double petY;

    protected PlayerColor color;
    protected Cosmetics.Hat hat = Cosmetics.Hat.NONE;
    protected Cosmetics.Skin skin = Cosmetics.Skin.DEFAULT;
    protected Cosmetics.Pet pet = Cosmetics.Pet.NONE;

    protected PlayerRole role;
    protected PlayerRole originalRole;
    protected boolean alive = true;
    protected boolean connected = true;
    protected boolean bodyReported = false;
    protected float taskProgress = 0.0f;
    protected MovementState movementState = MovementState.IDLE;
    protected RoomId currentRoom = RoomId.CAFETERIA;

    protected Character(String id, String name, double x, double y, PlayerColor color, PlayerRole role) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.deadBodyX = x;
        this.deadBodyY = y;
        this.petX = x - 34;
        this.petY = y + 10;
        this.color = color;
        this.role = role;
        this.originalRole = role;
    }

    public void update(double dt, CollisionManager collisionManager) {
        idleTime += dt;

        if (!alive) {
            deathTimer += dt;
            if (role == PlayerRole.GHOST) {
                movementState = MovementState.GHOST_FLOATING;
                applyVelocity(dt, null); // Ghosts pass smoothly through walls
            } else {
                movementState = MovementState.DEAD;
            }
            updatePetPosition(dt);
            return;
        }

        double speedLen = Math.hypot(vx, vy);
        if (speedLen > 1.0) {
            movementState = MovementState.WALKING;
            walkCycle += dt * 13.5;
            if (vx > 1.0) facingRight = true;
            if (vx < -1.0) facingRight = false;
            applyVelocity(dt, collisionManager);
        } else {
            movementState = MovementState.IDLE;
            walkCycle = 0.0;
        }

        updatePetPosition(dt);
    }

    private void applyVelocity(double dt, CollisionManager collisionManager) {
        double nextX = x + vx * dt;
        double nextY = y + vy * dt;

        if (collisionManager == null) {
            x = Math.max(80, Math.min(3120, nextX));
            y = Math.max(80, Math.min(1920, nextY));
            return;
        }

        if (collisionManager.canMoveTo(nextX, y, collisionRadius)) {
            x = nextX;
        }
        if (collisionManager.canMoveTo(x, nextY, collisionRadius)) {
            y = nextY;
        }
    }

    private void updatePetPosition(double dt) {
        if (pet == Cosmetics.Pet.NONE) return;
        double targetX = x + (facingRight ? -38 : 38);
        double targetY = y + 12;
        double lerp = Math.min(1.0, dt * 6.5);
        petX += (targetX - petX) * lerp;
        petY += (targetY - petY) * lerp;
    }

    public void setVelocity(double vx, double vy) {
        this.vx = vx;
        this.vy = vy;
    }

    public void kill() {
        this.alive = false;
        this.deathTimer = 0.0;
        this.deadBodyX = this.x;
        this.deadBodyY = this.y;
        this.hasUnreportedBody = true;
        this.bodyReported = false;
        this.vx = 0;
        this.vy = 0;
        this.role = PlayerRole.GHOST;
        this.movementState = MovementState.GHOST_FLOATING;
        this.moveSpeed = 305.0;
    }

    public void becomeGhost() {
        this.alive = false;
        this.role = PlayerRole.GHOST;
        this.movementState = MovementState.GHOST_FLOATING;
        this.moveSpeed = 305.0;
    }

    public void reviveAt(double spawnX, double spawnY, PlayerRole restoredRole) {
        this.x = spawnX;
        this.y = spawnY;
        this.deadBodyX = spawnX;
        this.deadBodyY = spawnY;
        this.petX = spawnX - 34;
        this.petY = spawnY + 10;
        this.alive = true;
        this.hasUnreportedBody = false;
        this.bodyReported = false;
        this.role = restoredRole;
        this.originalRole = restoredRole;
        this.moveSpeed = 265.0;
        this.movementState = MovementState.IDLE;
    }

    public double distanceTo(double targetX, double targetY) {
        return Math.hypot(this.x - targetX, this.y - targetY);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public double getDeadBodyX() { return deadBodyX; }
    public double getDeadBodyY() { return deadBodyY; }
    public boolean hasUnreportedBody() { return hasUnreportedBody; }
    public void setHasUnreportedBody(boolean hasUnreportedBody) { this.hasUnreportedBody = hasUnreportedBody; }

    public double getPetX() { return petX; }
    public double getPetY() { return petY; }

    public double getMoveSpeed() { return moveSpeed; }
    public void setMoveSpeed(double moveSpeed) { this.moveSpeed = moveSpeed; }

    public double getInteractionRange() { return interactionRange; }
    public void setInteractionRange(double interactionRange) { this.interactionRange = interactionRange; }

    public double getCollisionRadius() { return collisionRadius; }
    public boolean isFacingRight() { return facingRight; }
    public void setFacingRight(boolean facingRight) { this.facingRight = facingRight; }

    public double getWalkCycle() { return walkCycle; }
    public double getIdleTime() { return idleTime; }
    public double getDeathTimer() { return deathTimer; }

    public PlayerColor getColor() { return color; }
    public void setColor(PlayerColor color) { this.color = color; }

    public Cosmetics.Hat getHat() { return hat; }
    public void setHat(Cosmetics.Hat hat) { this.hat = hat; }

    public Cosmetics.Skin getSkin() { return skin; }
    public void setSkin(Cosmetics.Skin skin) { this.skin = skin; }

    public Cosmetics.Pet getPet() { return pet; }
    public void setPet(Cosmetics.Pet pet) { this.pet = pet; }

    public PlayerRole getRole() { return role; }
    public void setRole(PlayerRole role) {
        this.role = role;
        if (role != PlayerRole.GHOST) {
            this.originalRole = role;
        }
    }

    public PlayerRole getOriginalRole() { return originalRole; }

    public boolean isAlive() { return alive; }
    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) { this.connected = connected; }

    public boolean isBodyReported() { return bodyReported; }
    public void setBodyReported(boolean bodyReported) { this.bodyReported = bodyReported; }

    public float getTaskProgress() { return taskProgress; }
    public void setTaskProgress(float taskProgress) { this.taskProgress = Math.max(0f, Math.min(1f, taskProgress)); }

    public MovementState getMovementState() { return movementState; }
    public void setMovementState(MovementState movementState) { this.movementState = movementState; }

    public RoomId getCurrentRoom() { return currentRoom; }
    public void setCurrentRoom(RoomId currentRoom) { this.currentRoom = currentRoom; }
}