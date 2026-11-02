package dev.swarmmobs.algorithm;

import dev.swarmmobs.agent.SwarmLocalComposition;
import dev.swarmmobs.agent.SwarmRole;

/** Species-specific, observation-bound tactical posture for vanilla Zombies. */
public final class SwarmZombieTacticsPolicy {
    public enum Mode {
        SEARCH_SCOUT,
        FRONT_ANCHOR,
        FRONT_SCREEN,
        FLANK_GAP,
        SECONDARY_PRESSURE,
        REAR_RESERVE
    }

    public record Decision(Mode mode, double formationRadiusMultiplier) {}

    public static Decision choose(SwarmRole role, SwarmLocalComposition composition,
            SwarmCombinedArmsPolicy.Plan operation, boolean searchMode,
            double targetConfidence) {
        if (searchMode) return new Decision(Mode.SEARCH_SCOUT, 1.0);
        if (role == null || composition == null || !Double.isFinite(targetConfidence)
                || targetConfidence < 0.55) {
            return new Decision(Mode.FRONT_ANCHOR, 1.0);
        }
        if (role == SwarmRole.CHASER) {
            return operation != null && operation.zombieScreen()
                    ? new Decision(Mode.FRONT_SCREEN, 0.96)
                    : new Decision(Mode.FRONT_ANCHOR, 1.0);
        }
        if (role == SwarmRole.FLANK_LEFT || role == SwarmRole.FLANK_RIGHT) {
            return operation != null && operation.spiderPincer()
                    ? new Decision(Mode.SECONDARY_PRESSURE, 1.06)
                    : new Decision(Mode.FLANK_GAP, 1.0);
        }
        if (role == SwarmRole.REAR_PRESSURE) {
            return composition.rangedSupportCount() > 0
                    ? new Decision(Mode.REAR_RESERVE, 1.04)
                    : new Decision(Mode.SECONDARY_PRESSURE, 1.0);
        }
        return new Decision(Mode.FRONT_ANCHOR, 1.0);
    }

    private SwarmZombieTacticsPolicy() {}
}
