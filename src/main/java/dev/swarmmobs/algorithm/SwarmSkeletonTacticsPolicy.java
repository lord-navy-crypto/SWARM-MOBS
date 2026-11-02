package dev.swarmmobs.algorithm;

import dev.swarmmobs.agent.SwarmLocalComposition;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Stable overwatch and crossfire lane assignment for same-target Skeletons. */
public final class SwarmSkeletonTacticsPolicy {
    public enum Mode { SEARCH_RELAY, SOLO_SUPPORT, OVERWATCH, CROSS_FIRE }

    public record Decision(Mode mode, int preferredLaneSide,
            double formationRadiusMultiplier, int localSkeletonCount) {}

    public static Decision choose(UUID self, List<UUID> sameTargetSkeletons,
            SwarmLocalComposition composition,
            SwarmCombinedArmsPolicy.Plan operation,
            boolean searchMode, double confidence) {
        int side = preferredSide(self, sameTargetSkeletons);
        int count = uniqueCount(self, sameTargetSkeletons);
        if (searchMode) return new Decision(Mode.SEARCH_RELAY, side, 1.0, count);
        if (composition == null || !Double.isFinite(confidence) || confidence < 0.55) {
            return new Decision(Mode.SOLO_SUPPORT, side, 1.0, count);
        }
        if (count >= 2) return new Decision(Mode.CROSS_FIRE, side, 1.12, count);
        if (operation != null && operation.skeletonOverwatch()) {
            return new Decision(Mode.OVERWATCH, side, 1.08, count);
        }
        return new Decision(Mode.SOLO_SUPPORT, side, 1.0, count);
    }

    /** Opposite signs are assigned to adjacent UUID-ranked Skeletons. */
    public static int preferredSide(UUID self, List<UUID> peers) {
        if (self == null) return 1;
        List<UUID> members = new ArrayList<>();
        members.add(self);
        if (peers != null) for (UUID peer : peers) {
            if (peer != null && !members.contains(peer)) members.add(peer);
        }
        members.sort(UUID::compareTo);
        int rank = members.indexOf(self);
        return Math.floorMod(rank, 2) == 0 ? -1 : 1;
    }

    private static int uniqueCount(UUID self, List<UUID> peers) {
        if (self == null) return peers == null ? 0 : (int) peers.stream().filter(x -> x != null).distinct().count();
        List<UUID> members = new ArrayList<>();
        members.add(self);
        if (peers != null) for (UUID peer : peers) {
            if (peer != null && !members.contains(peer)) members.add(peer);
        }
        return members.size();
    }

    private SwarmSkeletonTacticsPolicy() {}
}
