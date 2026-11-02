package dev.swarmmobs.bee;

/**
 * Pure utility function for comparing remembered flower patches.
 * A calling adapter must gather these values through bounded bee observations.
 */
public final class BeeForagePolicy {
    private BeeForagePolicy() {}

    public record Patch(double nectarYield, double pollenYield, double distance,
                        double competition, double danger, double confidence) {
        public Patch {
            unit(nectarYield); unit(pollenYield); unit(competition);
            unit(danger); unit(confidence);
            if (!Double.isFinite(distance) || distance < 0)
                throw new IllegalArgumentException("distance must be non-negative and finite");
        }

        private static void unit(double value) {
            if (!Double.isFinite(value) || value < 0 || value > 1)
                throw new IllegalArgumentException("normalized values must be in [0,1]");
        }
    }

    /** Score is bounded and reproducible for identical observations. */
    public static double score(Patch patch) {
        double reward = patch.nectarYield() * 0.6 + patch.pollenYield() * 0.4;
        double costs = Math.min(1.0, patch.distance() / 128.0) * 0.25
                + patch.competition() * 0.3 + patch.danger() * 0.65;
        return patch.confidence() * reward - costs;
    }

    /** Exponential decay avoids knowledge of changes the bee has not observed. */
    public static double confidenceAfter(long elapsedTicks, double initial, long halfLifeTicks) {
        if (elapsedTicks < 0 || halfLifeTicks <= 0 || !Double.isFinite(initial)
                || initial < 0 || initial > 1)
            throw new IllegalArgumentException("Invalid memory decay inputs");
        return initial * Math.pow(0.5, (double) elapsedTicks / halfLifeTicks);
    }
}
