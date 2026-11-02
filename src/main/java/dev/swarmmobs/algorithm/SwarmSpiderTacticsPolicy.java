package dev.swarmmobs.algorithm;

import dev.swarmmobs.agent.SwarmLocalComposition;
import dev.swarmmobs.agent.SwarmRole;
import dev.swarmmobs.algorithm.SwarmAdaptiveTacticsPolicy.Frame;
import dev.swarmmobs.algorithm.SwarmAdaptiveTacticsPolicy.Pattern;
import dev.swarmmobs.algorithm.SwarmCombatPlanner.Vec2;

/** Side-specific pincer and arc movement for vanilla Minecraft Spiders. */
public final class SwarmSpiderTacticsPolicy {
    public enum Mode { SEARCH_SCOUT, SOLO_FLANK, WIDE_FLANK, PAIRED_PINCER }

    public record Decision(Mode mode, double formationRadiusMultiplier) {}

    public static Decision choose(SwarmLocalComposition composition,
            SwarmCombinedArmsPolicy.Plan operation, boolean searchMode,
            double confidence) {
        if (searchMode) return new Decision(Mode.SEARCH_SCOUT, 1.0);
        if (composition == null || !Double.isFinite(confidence) || confidence < 0.55) {
            return new Decision(Mode.SOLO_FLANK, 1.0);
        }
        if (operation != null && operation.spiderPincer()
                && composition.flankerCount() >= 2) {
            return new Decision(Mode.PAIRED_PINCER, 1.14);
        }
        if (composition.assaultCount() > 0) {
            return new Decision(Mode.WIDE_FLANK, 1.08);
        }
        return new Decision(Mode.SOLO_FLANK, 1.0);
    }

    /** During SURROUND, Spiders take a short arc beyond the target's flank. */
    public static Vec2 refineDestination(Frame frame, Vec2 original,
            SwarmRole role, double formationRadius) {
        if (frame == null || frame.pattern() != Pattern.SURROUND
                || (role != SwarmRole.FLANK_LEFT && role != SwarmRole.FLANK_RIGHT)
                || !finite(original) || !finite(frame.forward())
                || !Double.isFinite(formationRadius) || formationRadius <= 0.0) {
            return original;
        }
        double arcLead = Math.min(1.75, formationRadius * 0.30);
        return original.add(frame.forward().scale(arcLead));
    }

    private static boolean finite(Vec2 p) {
        return p != null && Double.isFinite(p.x()) && Double.isFinite(p.z());
    }

    private SwarmSpiderTacticsPolicy() {}
}
