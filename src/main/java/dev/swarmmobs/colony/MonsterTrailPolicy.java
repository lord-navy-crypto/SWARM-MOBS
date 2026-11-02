package dev.swarmmobs.colony;

/**
 * Termite/ant-inspired decaying trail evidence. The value is an abstract local
 * navigation preference, NOT a world-wide path or a block mutation.
 */
public final class MonsterTrailPolicy {
    private MonsterTrailPolicy() {}

    public static double decay(double strength, long elapsedTicks, long halfLifeTicks) {
        unit(strength);
        if (elapsedTicks < 0 || halfLifeTicks <= 0)
            throw new IllegalArgumentException("Invalid trail age");
        return strength * Math.pow(0.5, (double) elapsedTicks / halfLifeTicks);
    }

    public static double reinforce(double current, double deposited, double capacity) {
        unit(current); unit(deposited);
        if (!Double.isFinite(capacity) || capacity <= 0 || capacity > 1)
            throw new IllegalArgumentException("Capacity must be finite in (0,1]");
        return Math.min(capacity, current + deposited * (capacity - current));
    }

    public static double routeUtility(double trailStrength, double distancePenalty,
                                      double danger, double congestion) {
        unit(trailStrength); unit(distancePenalty); unit(danger); unit(congestion);
        return trailStrength - 0.3 * distancePenalty - 0.7 * danger - 0.5 * congestion;
    }

    private static void unit(double value) {
        if (!Double.isFinite(value) || value < 0 || value > 1)
            throw new IllegalArgumentException("Expected normalized [0,1] value");
    }
}
