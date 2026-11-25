package starfall.gameplay;

import java.util.List;
import starfall.entities.Character;
import starfall.entities.Player;
import starfall.entities.PlayerRole;

public class WinConditionManager {

    public enum Faction {
        CREWMATES,
        IMPOSTORS,
        DRAW
    }

    public static class MatchOutcome {
        private final Faction winner;
        private final String headline;
        private final String reason;

        public MatchOutcome(Faction winner, String headline, String reason) {
            this.winner = winner;
            this.headline = headline;
            this.reason = reason;
        }

        public Faction getWinner() { return winner; }
        public String getHeadline() { return headline; }
        public String getReason() { return reason; }
    }

    public MatchOutcome evaluate(List<Player> players, TaskManager taskManager, SabotageManager sabotageManager) {
        if (players == null || players.isEmpty()) {
            return new MatchOutcome(Faction.DRAW, "MATCH ABORTED", "No active players connected.");
        }

        int connectedCount = 0;
        int aliveCrewmates = 0;
        int aliveImpostors = 0;
        int totalImpostors = 0;

        for (Player p : players) {
            Character c = p.getCharacter();
            if (!c.isConnected()) continue;
            connectedCount++;

            PlayerRole baseRole = c.getOriginalRole();
            if (baseRole == PlayerRole.IMPOSTOR) {
                totalImpostors++;
                if (c.isAlive()) aliveImpostors++;
            } else if (baseRole == PlayerRole.CREWMATE && c.isAlive()) {
                aliveCrewmates++;
            }
        }

        if (connectedCount == 0) {
            return new MatchOutcome(Faction.DRAW, "MATCH ENDED", "All players disconnected from the session.");
        }

        if (totalImpostors == 0) {
            return new MatchOutcome(Faction.CREWMATES, "CREWMATES VICTORY", "No Impostors remain aboard STARFALL-I.");
        }

        boolean criticalSabotageFailed = sabotageManager.isCriticalFailureTriggered();
        boolean tasksCompleted = taskManager.isAllTasksCompleted();

        if (criticalSabotageFailed && tasksCompleted) {
            return new MatchOutcome(Faction.DRAW, "DRAW — MUTUAL CATASTROPHE",
                    "Final task finished at the exact instant critical systems failed!");
        }

        if (criticalSabotageFailed) {
            return new MatchOutcome(Faction.IMPOSTORS, "IMPOSTORS VICTORY",
                    "Crew failed to resolve a critical ship sabotage in time!");
        }

        if (aliveImpostors == 0) {
            return new MatchOutcome(Faction.CREWMATES, "CREWMATES VICTORY",
                    "All Impostors have been eliminated from the ship!");
        }

        if (aliveImpostors >= aliveCrewmates) {
            return new MatchOutcome(Faction.IMPOSTORS, "IMPOSTORS VICTORY",
                    "Impostors have reached numerical parity (" + aliveImpostors + " vs " + aliveCrewmates + ")!");
        }

        if (tasksCompleted) {
            return new MatchOutcome(Faction.CREWMATES, "CREWMATES VICTORY",
                    "All required ship tasks were completed!");
        }

        return null;
    }
}