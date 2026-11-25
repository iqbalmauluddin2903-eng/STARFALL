package starfall.gameplay;

import java.util.*;
import starfall.entities.Character;
import starfall.entities.Player;
import starfall.utils.Logger;

public class VotingManager {

    public static final String SKIP_VOTE = "SKIP";

    private final Map<String, String> votesByVoter = new LinkedHashMap<>();
    private String pendingTargetId = null;
    private boolean finalized = false;

    private String ejectedPlayerId = null;
    private boolean wasTie = false;
    private boolean wasSkipped = false;

    public void reset() {
        votesByVoter.clear();
        pendingTargetId = null;
        finalized = false;
        ejectedPlayerId = null;
        wasTie = false;
        wasSkipped = false;
    }

    public void selectPendingTarget(String targetId) {
        if (!finalized) {
            this.pendingTargetId = targetId;
        }
    }

    public void clearPendingTarget() {
        this.pendingTargetId = null;
    }

    public String getPendingTargetId() {
        return pendingTargetId;
    }

    public void forceCastVote(String voterId, String targetId, List<Player> players) {
        if (voterId != null && targetId != null) {
            votesByVoter.put(voterId, targetId);
            if (voterId.equals(getLocalPlayerId(players))) {
                pendingTargetId = null;
            }
        }
    }

    public void applyServerResult(String ejectedId, boolean tie, boolean skipped, String rawVotePairs) {
        this.finalized = true;
        this.pendingTargetId = null;
        this.ejectedPlayerId = (ejectedId == null || ejectedId.isBlank()) ? null : ejectedId;
        this.wasTie = tie;
        this.wasSkipped = skipped;

        votesByVoter.clear();
        if (rawVotePairs != null && !rawVotePairs.isBlank()) {
            for (String pair : rawVotePairs.split(";")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    votesByVoter.put(kv[0], kv[1]);
                }
            }
        }
    }

    public boolean castVote(String voterId, String targetId, List<Player> players) {
        if (finalized || voterId == null || targetId == null) return false;
        if (votesByVoter.containsKey(voterId)) return false;

        Player voter = findPlayerById(players, voterId);
        if (voter == null || !voter.getCharacter().isAlive()) return false;

        if (!SKIP_VOTE.equals(targetId)) {
            Player target = findPlayerById(players, targetId);
            if (target == null || !target.getCharacter().isAlive()) return false;
        }

        votesByVoter.put(voterId, targetId);
        if (voterId.equals(getLocalPlayerId(players))) {
            pendingTargetId = null;
        }
        Logger.info("Vote recorded: " + voter.getCharacter().getName() + " -> " + targetId);
        return true;
    }

    public boolean hasVoted(String voterId) {
        return votesByVoter.containsKey(voterId);
    }

    public boolean allLivingPlayersVoted(List<Player> players) {
        for (Player p : players) {
            Character c = p.getCharacter();
            if (c.isAlive() && c.isConnected() && !votesByVoter.containsKey(p.getPlayerId())) {
                return false;
            }
        }
        return true;
    }

    public void finalizeVotes(List<Player> players) {
        if (finalized) return;
        finalized = true;
        pendingTargetId = null;

        Map<String, Integer> tally = new LinkedHashMap<>();
        tally.put(SKIP_VOTE, 0);
        for (Player p : players) {
            if (p.getCharacter().isAlive()) {
                tally.put(p.getPlayerId(), 0);
            }
        }

        for (Map.Entry<String, String> entry : votesByVoter.entrySet()) {
            Player voter = findPlayerById(players, entry.getKey());
            if (voter == null || !voter.getCharacter().isAlive()) continue;
            String target = entry.getValue();
            if (tally.containsKey(target)) {
                tally.put(target, tally.get(target) + 1);
            }
        }

        int maxVotes = 0;
        for (int count : tally.values()) {
            if (count > maxVotes) maxVotes = count;
        }

        if (maxVotes == 0) {
            wasSkipped = true;
            ejectedPlayerId = null;
            return;
        }

        List<String> topCandidates = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : tally.entrySet()) {
            if (entry.getValue() == maxVotes) {
                topCandidates.add(entry.getKey());
            }
        }

        if (topCandidates.size() > 1) {
            wasTie = true;
            ejectedPlayerId = null;
        } else {
            String winner = topCandidates.get(0);
            if (SKIP_VOTE.equals(winner)) {
                wasSkipped = true;
                ejectedPlayerId = null;
            } else {
                ejectedPlayerId = winner;
            }
        }
    }

    public List<Player> getVotersFor(String targetId, List<Player> players) {
        List<Player> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : votesByVoter.entrySet()) {
            if (targetId.equals(entry.getValue())) {
                Player voter = findPlayerById(players, entry.getKey());
                if (voter != null) result.add(voter);
            }
        }
        return result;
    }

    private Player findPlayerById(List<Player> players, String id) {
        for (Player p : players) {
            if (p.getPlayerId().equals(id)) return p;
        }
        return null;
    }

    private String getLocalPlayerId(List<Player> players) {
        for (Player p : players) {
            if (p.isLocalPlayer()) return p.getPlayerId();
        }
        return "";
    }

    public boolean isFinalized() { return finalized; }
    public String getEjectedPlayerId() { return ejectedPlayerId; }
    public boolean isWasTie() { return wasTie; }
    public boolean isWasSkipped() { return wasSkipped; }
}