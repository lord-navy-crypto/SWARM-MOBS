package dev.swarmmobs.algorithm;

import dev.swarmmobs.algorithm.SwarmCombatPlanner.Vec2;
import java.util.function.ToDoubleFunction;

/** Redirects stale-target search toward a nearby sector with less prior coverage. */
public final class SwarmSearchCoveragePolicy {
    private static final double ANGLE_STEP = Math.PI / 6.0;
    private static final double MIN_BASE_COVERAGE = 0.35;
    private static final double MIN_SCORE_IMPROVEMENT = 0.15;
    private static final double SECTOR_SHIFT_COST = 0.08;
    private static final int[] EVEN_ORDER = {1, -1, 2, -2};
    private static final int[] ODD_ORDER = {-1, 1, -2, 2};

    public record Adjustment(SwarmSearchPlanner.SearchPlan plan, int sectorShift,
            double originalCoverage, double selectedCoverage) {}

    public static Adjustment adjust(SwarmSearchPlanner.SearchPlan plan,
            Vec2 lastKnownTarget, int stableSlot,
            ToDoubleFunction<Vec2> coverageAt, boolean enabled) {
        Adjustment unchanged = new Adjustment(plan, 0, 0.0, 0.0);
        if (!enabled || plan == null || !finite(plan.destination())
                || !finite(lastKnownTarget) || !Double.isFinite(plan.searchRadius())
                || plan.searchRadius() <= 0.0 || !Double.isFinite(plan.angleRadians())
                || coverageAt == null) return unchanged;

        Vec2 originalAnchor = point(lastKnownTarget, plan.angleRadians(), plan.searchRadius());
        double originalCoverage = sample(coverageAt, originalAnchor);
        if (originalCoverage < MIN_BASE_COVERAGE) {
            return new Adjustment(plan, 0, originalCoverage, originalCoverage);
        }

        double bestScore = originalCoverage;
        double selectedCoverage = originalCoverage;
        int selectedShift = 0;
        int[] order = Math.floorMod(stableSlot, 2) == 0 ? EVEN_ORDER : ODD_ORDER;
        for (int shift : order) {
            double angle = plan.angleRadians() + shift * ANGLE_STEP;
            Vec2 candidate = point(lastKnownTarget, angle, plan.searchRadius());
            double coverage = sample(coverageAt, candidate);
            double score = coverage + Math.abs(shift) * SECTOR_SHIFT_COST;
            if (score < bestScore) {
                bestScore = score;
                selectedCoverage = coverage;
                selectedShift = shift;
            }
        }

        if (selectedShift == 0 || originalCoverage - bestScore < MIN_SCORE_IMPROVEMENT) {
            return new Adjustment(plan, 0, originalCoverage, originalCoverage);
        }
        double newAngle = plan.angleRadians() + selectedShift * ANGLE_STEP;
        Vec2 newAnchor = point(lastKnownTarget, newAngle, plan.searchRadius());
        Vec2 displacement = newAnchor.subtract(originalAnchor);
        var adjusted = new SwarmSearchPlanner.SearchPlan(
                plan.destination().add(displacement), plan.searchRadius(), newAngle,
                plan.separationMagnitude(), plan.cohesionMagnitude(),
                plan.alignmentMagnitude(), plan.steeringMagnitude());
        return new Adjustment(adjusted, selectedShift, originalCoverage, selectedCoverage);
    }

    private static Vec2 point(Vec2 center, double angle, double radius) {
        return center.add(new Vec2(Math.cos(angle), Math.sin(angle)).scale(radius));
    }

    private static double sample(ToDoubleFunction<Vec2> provider, Vec2 point) {
        double value = provider.applyAsDouble(point);
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }

    private static boolean finite(Vec2 point) {
        return point != null && Double.isFinite(point.x()) && Double.isFinite(point.z());
    }

    private SwarmSearchCoveragePolicy() {}
}