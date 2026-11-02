package dev.swarmmobs.algorithm;

import dev.swarmmobs.agent.SwarmAgentArchetype;

/**
 * Species-specific locomotion tuning only. This policy changes no attacks,
 * targeting, damage, projectile behavior, or Creeper fuse rules.
 */
public final class SwarmSpeciesNavigationPolicy {
    public record Profile(
            double obstacleLateralScale,
            double recoveryLateralScale,
            double congestionPenaltyScale,
            int repeatedFailureThreshold,
            long failureMemoryTicks
    ) {
        public Profile {
            if (!Double.isFinite(obstacleLateralScale) || obstacleLateralScale <= 0
                    || !Double.isFinite(recoveryLateralScale) || recoveryLateralScale <= 0
                    || !Double.isFinite(congestionPenaltyScale) || congestionPenaltyScale < 0
                    || repeatedFailureThreshold < 1 || failureMemoryTicks < 1) {
                throw new IllegalArgumentException("invalid species navigation profile");
            }
        }
    }

    public static Profile profile(SwarmAgentArchetype archetype) {
        if (archetype == null) return profile(SwarmAgentArchetype.ASSAULT);
        return switch (archetype) {
            case FLANKER ->
                // Spiders tolerate wider local detours but retain a short
                // memory, allowing native climbing/navigation to try again soon.
                    new Profile(1.25, 1.30, 0.90, 2, 100);
            case RANGED_SUPPORT ->
                // Cautious locomotion spacing; no changes to ranged attack behavior.
                    new Profile(1.10, 1.10, 1.15, 2, 160);
            case BREACHER ->
                // Creepers prefer fewer route changes and longer failed-route memory.
                    new Profile(0.85, 0.85, 1.10, 3, 200);
            case ASSAULT ->
                    new Profile(1.0, 1.0, 1.0, 2, 140);
        };
    }

    private SwarmSpeciesNavigationPolicy() {}
}
