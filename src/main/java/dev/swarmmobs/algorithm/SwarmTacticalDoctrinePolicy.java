package dev.swarmmobs.algorithm;

import dev.swarmmobs.agent.SwarmAgentArchetype;
import dev.swarmmobs.agent.SwarmLocalComposition;

/**
 * Selects a small combined-arms formation doctrine from the locally observed
 * same-target Minecraft squad. It adjusts spacing only; role assignment and
 * native game behavior remain with their existing controllers.
 */
public final class SwarmTacticalDoctrinePolicy {
    public enum Doctrine {
        STANDARD,
        SEARCH_GRID,
        RANGED_SCREEN,
        PINCER,
        BREACH_SUPPORT,
        FULL_COMBINED_ARMS
    }

    public record Decision(Doctrine doctrine, int squadSize) {}

    public static Decision choose(SwarmLocalComposition composition,
            int squadSize, boolean searchMode, double targetConfidence,
            boolean enabled) {
        int size = Math.max(1, squadSize);
        if (searchMode) return new Decision(Doctrine.SEARCH_GRID, size);
        if (!enabled || composition == null || !Double.isFinite(targetConfidence)
                || targetConfidence < 0.65) {
            return new Decision(Doctrine.STANDARD, size);
        }
        if (size >= 4 && composition.assaultCount() > 0
                && composition.rangedSupportCount() > 0
                && composition.flankerCount() > 0
                && composition.breacherCount() > 0) {
            return new Decision(Doctrine.FULL_COMBINED_ARMS, size);
        }
        if (size >= 3 && composition.assaultCount() > 0
                && composition.rangedSupportCount() > 0
                && composition.breacherCount() > 0) {
            return new Decision(Doctrine.BREACH_SUPPORT, size);
        }
        if (size >= 3 && composition.assaultCount() >= 2
                && composition.flankerCount() > 0) {
            return new Decision(Doctrine.PINCER, size);
        }
        if (size >= 2 && composition.assaultCount() > 0
                && composition.rangedSupportCount() > 0) {
            return new Decision(Doctrine.RANGED_SCREEN, size);
        }
        return new Decision(Doctrine.STANDARD, size);
    }

    public static double formationRadiusMultiplier(Decision decision,
            SwarmAgentArchetype archetype) {
        if (decision == null || decision.doctrine() == null || archetype == null) return 1.0;
        return switch (decision.doctrine()) {
            case FULL_COMBINED_ARMS -> switch (archetype) {
                case ASSAULT -> 0.94;
                case RANGED_SUPPORT -> 1.22;
                case FLANKER -> 1.30;
                case BREACHER -> 0.88;
            };
            case BREACH_SUPPORT -> switch (archetype) {
                case ASSAULT -> 1.0;
                case RANGED_SUPPORT -> 1.25;
                case FLANKER -> 1.10;
                case BREACHER -> 0.88;
            };
            case PINCER -> switch (archetype) {
                case ASSAULT -> 0.96;
                case RANGED_SUPPORT -> 1.12;
                case FLANKER -> 1.28;
                case BREACHER -> 1.0;
            };
            case RANGED_SCREEN -> switch (archetype) {
                case ASSAULT -> 0.96;
                case RANGED_SUPPORT -> 1.22;
                case FLANKER, BREACHER -> 1.0;
            };
            case STANDARD, SEARCH_GRID -> 1.0;
        };
    }

    private SwarmTacticalDoctrinePolicy() {}
}