package dev.swarmmobs.algorithm;

/**
 * Converts an operator-selected relative natural-spawn weight multiplier into
 * a bounded integer weight. Zero removes the selected species from the
 * natural biome candidate list; one preserves the vanilla weight.
 */
public final class SwarmSpawnWeightPolicy {
    public static final double MIN_MULTIPLIER = 0.0;
    public static final double MAX_MULTIPLIER = 4.0;
    public static final int MAX_WEIGHT = 4096;

    public static int adjustedWeight(int vanillaWeight, double multiplier) {
        if (vanillaWeight <= 0) return 0;
        if (!Double.isFinite(multiplier)) return vanillaWeight;
        double bounded = Math.max(MIN_MULTIPLIER,
                Math.min(MAX_MULTIPLIER, multiplier));
        if (bounded <= 0.0) return 0;
        long adjusted = Math.round(vanillaWeight * bounded);
        return (int) Math.max(1L, Math.min(MAX_WEIGHT, adjusted));
    }

    private SwarmSpawnWeightPolicy() {}
}
