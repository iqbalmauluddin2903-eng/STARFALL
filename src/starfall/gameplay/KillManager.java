package starfall.gameplay;

import starfall.entities.Character;
import starfall.entities.Impostor;
import starfall.entities.Player;
import starfall.entities.PlayerRole;
import starfall.utils.Logger;

import java.util.List;

public class KillManager {

    public Character findNearestKillTarget(Impostor impostor, List<Player> players) {
        if (!impostor.isAlive() || impostor.isInsideVent()) return null;

        Character best = null;
        double bestDist = impostor.getKillRange();

        for (Player p : players) {
            Character c = p.getCharacter();
            if (c == impostor || !c.isAlive()) continue;
            if (c.getRole() == PlayerRole.IMPOSTOR || c.getRole() == PlayerRole.GHOST) continue;

            double dist = impostor.distanceTo(c.getX(), c.getY());
            if (dist <= bestDist) {
                bestDist = dist;
                best = c;
            }
        }
        return best;
    }

    public boolean executeKill(Impostor impostor, Character target) {
        if (impostor == null || target == null || !impostor.canKill() || !target.isAlive()) {
            return false;
        }

        // Lunge impostor onto victim position like original Among Us
        impostor.setX(target.getX());
        impostor.setY(target.getY());
        target.kill();
        impostor.triggerKillCooldown();

        Logger.warn(impostor.getName() + " eliminated " + target.getName() + " in " + target.getCurrentRoom().getDisplayName());
        return true;
    }
}