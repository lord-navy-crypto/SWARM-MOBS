package dev.swarmmobs.algorithm;

/**
 * Time-normalized specialization experience.
 *
 * Gain and retention are calibrated to the original six-tick planning interval,
 * but fractional intervals compose exactly. Changing the planner cadence no
 * longer changes how quickly an agent learns or forgets a task.
 */
public final class SwarmTaskExperiencePolicy {
    public static final int REFERENCE_INTERVAL_TICKS = 6;
    public static final int MAX_CATCH_UP_TICKS = 40;

    public static double advance(
            double current,
            double gainPerReferenceInterval,
            double retentionPerReferenceInterval,
            long elapsedTicks
    ) {
        double boundedCurrent = clamp(current, 0.0, 1.0);
        if (elapsedTicks <= 0L) return boundedCurrent;

        long boundedTicks = Math.min(MAX_CATCH_UP_TICKS, elapsedTicks);
        double intervals = boundedTicks / (double) REFERENCE_INTERVAL_TICKS;
        double gain = clamp(gainPerReferenceInterval, 0.0, 1.0);
        double retention = clamp(retentionPerReferenceInterval, 0.90, 1.0);
        double retentionFactor = Math.pow(retention, intervals);

        double gainContribution;
        double lossPerInterval = 1.0 - retention;
        if (lossPerInterval <= 1.0e-12) {
            gainContribution = gain * intervals;
        } else {
            gainContribution = gain * (1.0 - retentionFactor) / lossPerInterval;
        }

        return clamp(
                boundedCurrent * retentionFactor + gainContribution,
                0.0,
                1.0
        );
    }

    private static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) return min;
        return Math.max(min, Math.min(max, value));
    }

    private SwarmTaskExperiencePolicy() {}
}
