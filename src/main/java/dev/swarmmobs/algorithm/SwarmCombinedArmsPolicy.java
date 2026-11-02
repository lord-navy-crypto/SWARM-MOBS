package dev.swarmmobs.algorithm;

import dev.swarmmobs.agent.SwarmLocalComposition;

/**
 * Decentralized operation selection for one locally observed, same-target
 * Minecraft group. Every member derives the same bounded operation from its
 * own peer snapshot; there is no global commander or hidden target position.
 */
public final class SwarmCombinedArmsPolicy {
    public enum Operation {
        INDEPENDENT,
        SEARCH_RELAY,
        FRONTLINE_OVERWATCH,
        PINCER_SCREEN,
        BREACH_SCREEN,
        FULL_COMBINED_ARMS
    }

    public record Plan(
            Operation operation,
            int squadSize,
            boolean zombieScreen,
            boolean skeletonOverwatch,
            boolean spiderPincer,
            boolean creeperStaging
    ) {}

    public static Plan choose(SwarmLocalComposition composition, int squadSize,
            boolean searchMode, double targetConfidence) {
        int size = Math.max(1, squadSize);
        if (searchMode) {
            return new Plan(Operation.SEARCH_RELAY, size,
                    false, false, false, false);
        }
        if (composition == null || !Double.isFinite(targetConfidence)
                || targetConfidence < 0.65) {
            return new Plan(Operation.INDEPENDENT, size,
                    false, false, false, false);
        }

        boolean assault = composition.assaultCount() > 0;
        boolean ranged = composition.rangedSupportCount() > 0;
        boolean flank = composition.flankerCount() > 0;
        boolean breach = composition.breacherCount() > 0;

        if (size >= 4 && assault && ranged && flank && breach) {
            return new Plan(Operation.FULL_COMBINED_ARMS, size,
                    true, true, true, true);
        }
        if (assault && ranged && breach) {
            return new Plan(Operation.BREACH_SCREEN, size,
                    true, true, flank, true);
        }
        if (assault && flank && ranged) {
            return new Plan(Operation.PINCER_SCREEN, size,
                    true, true, true, false);
        }
        if (assault && ranged) {
            return new Plan(Operation.FRONTLINE_OVERWATCH, size,
                    true, true, flank, false);
        }
        if (assault && flank) {
            return new Plan(Operation.PINCER_SCREEN, size,
                    false, false, true, false);
        }
        return new Plan(Operation.INDEPENDENT, size,
                false, false, false, false);
    }

    private SwarmCombinedArmsPolicy() {}
}
