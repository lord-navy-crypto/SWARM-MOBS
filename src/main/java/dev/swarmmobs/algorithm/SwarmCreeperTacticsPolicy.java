package dev.swarmmobs.algorithm;

import dev.swarmmobs.algorithm.SwarmCombatPlanner.Vec2;
import java.util.List;

/** Staged ingress that preserves the vanilla Creeper fuse and explosion AI. */
public final class SwarmCreeperTacticsPolicy {
    public enum Mode { SEARCH, APPROACH, STAGED_INGRESS, VANILLA_FUSE }
    public record Decision(Mode mode, double formationRadiusMultiplier) {}

    public static final double STAGING_START_DISTANCE = 7.0;
    public static final double STAGING_END_DISTANCE = 16.0;
    public static final double STAGING_STANDOFF = 4.5;
    public static final double STAGING_LATERAL = 2.0;
    public static final double STAGING_ARRIVAL = 1.35;

    public static Decision choose(SwarmCombinedArmsPolicy.Plan operation,
            boolean searchMode, boolean directlyVisible, double confidence,
            double targetDistance, boolean fusing) {
        if (fusing) return new Decision(Mode.VANILLA_FUSE, 1.0);
        if (searchMode) return new Decision(Mode.SEARCH, 1.0);
        boolean canStage = operation != null && operation.creeperStaging()
                && directlyVisible && Double.isFinite(confidence)
                && confidence >= 0.70 && Double.isFinite(targetDistance)
                && targetDistance > STAGING_START_DISTANCE
                && targetDistance <= STAGING_END_DISTANCE;
        return canStage
                ? new Decision(Mode.STAGED_INGRESS, 0.94)
                : new Decision(Mode.APPROACH, 1.0);
    }

    /**
     * The approach axis is taken from the shared tactical frame so the staging
     * point does not rotate around the target as the Creeper moves. The caller
     * validates loaded-world footing and every active Skeleton firing lane.
     */
    public static Vec2 stagingWaypoint(Vec2 self, Vec2 target, Vec2 approachAxis,
            List<Vec2> sameTargetAllies, int stableSlot) {
        if (!finite(self) || !finite(target)) return null;
        double distance = self.subtract(target).length();
        if (!Double.isFinite(distance) || distance <= STAGING_START_DISTANCE
                || distance > STAGING_END_DISTANCE) return null;
        Vec2 forward = finite(approachAxis) && approachAxis.length() > 1.0e-6
                ? approachAxis.normalized() : target.subtract(self).normalized();
        Vec2 side = new Vec2(-forward.z(), forward.x());
        Vec2 base = target.subtract(forward.scale(STAGING_STANDOFF));
        Vec2 left = base.add(side.scale(STAGING_LATERAL));
        Vec2 right = base.add(side.scale(-STAGING_LATERAL));
        if (self.subtract(left).length() <= STAGING_ARRIVAL
                || self.subtract(right).length() <= STAGING_ARRIVAL) return null;

        double leftScore = score(left, self, sameTargetAllies);
        double rightScore = score(right, self, sameTargetAllies);
        if (Math.abs(leftScore - rightScore) < 1.0e-6) {
            return Math.floorMod(stableSlot, 2) == 0 ? left : right;
        }
        return leftScore < rightScore ? left : right;
    }

    private static double score(Vec2 candidate, Vec2 self, List<Vec2> allies) {
        double score = candidate.subtract(self).length();
        if (allies != null) for (Vec2 ally : allies) {
            if (!finite(ally)) continue;
            double distance = candidate.subtract(ally).length();
            if (distance < 3.0) score += (3.0 - distance) * 4.0;
        }
        return score;
    }

    private static boolean finite(Vec2 p) {
        return p != null && Double.isFinite(p.x()) && Double.isFinite(p.z());
    }

    private SwarmCreeperTacticsPolicy() {}
}
