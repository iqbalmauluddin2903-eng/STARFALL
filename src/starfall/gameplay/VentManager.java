package starfall.gameplay;

import starfall.entities.Impostor;
import starfall.entities.MovementState;
import starfall.map.GameMap;
import starfall.map.Vent;
import starfall.utils.Logger;

import java.util.List;

public class VentManager {

    public boolean toggleVent(Impostor impostor, GameMap map) {
        if (impostor == null || !impostor.isAlive()) return false;

        if (impostor.isInsideVent()) {
            impostor.setInsideVent(false);
            impostor.setCurrentVentId(null);
            impostor.setMovementState(MovementState.IDLE);
            Logger.info(impostor.getName() + " exited vent in " + impostor.getCurrentRoom().getDisplayName());
            return true;
        }

        Vent nearest = map.findNearestVent(impostor.getX(), impostor.getY(), impostor.getInteractionRange());
        if (nearest != null) {
            impostor.setX(nearest.getX());
            impostor.setY(nearest.getY());
            impostor.setVelocity(0, 0);
            impostor.setInsideVent(true);
            impostor.setCurrentVentId(nearest.getVentId());
            impostor.setMovementState(MovementState.VENTING);
            Logger.info(impostor.getName() + " entered vent: " + nearest.getVentId());
            return true;
        }
        return false;
    }

    public boolean cycleVent(Impostor impostor, GameMap map, int direction) {
        if (impostor == null || !impostor.isInsideVent() || impostor.getCurrentVentId() == null) {
            return false;
        }

        Vent current = findVentById(map, impostor.getCurrentVentId());
        if (current == null || current.getConnectedVentIds().isEmpty()) return false;

        List<String> targets = current.getConnectedVentIds();
        int idx = Math.floorMod(direction, targets.size());
        Vent nextVent = findVentById(map, targets.get(idx));

        if (nextVent != null) {
            impostor.setX(nextVent.getX());
            impostor.setY(nextVent.getY());
            impostor.setCurrentVentId(nextVent.getVentId());
            impostor.setCurrentRoom(nextVent.getRoomId());
            Logger.info(impostor.getName() + " vented to " + nextVent.getRoomId().getDisplayName());
            return true;
        }
        return false;
    }

    public Vent findVentById(GameMap map, String ventId) {
        for (Vent v : map.getVents()) {
            if (v.getVentId().equals(ventId)) return v;
        }
        return null;
    }
}