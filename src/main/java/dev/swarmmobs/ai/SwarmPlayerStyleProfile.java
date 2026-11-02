package dev.swarmmobs.ai;

/**
 * Pure session-local player-style accumulator. Keeping the decay and expiry
 * model independent from Minecraft world objects makes its time behavior
 * directly unit-testable.
 */
final class SwarmPlayerStyleProfile {
    static final long HALF_LIFE_TICKS = 2400L;
    static final long PROFILE_TTL_TICKS = 12000L;

    private final double[] counts = new double[6];
    private double sampleMass;
    private long lastDecayTick;
    private long lastObservedTick;

    SwarmPlayerStyleProfile(long now) {
        lastDecayTick = now;
        lastObservedTick = now;
    }

    void observe(SwarmPlayerStyleRegistry.AttackStyle style, long now) {
        if (style == null
                || style == SwarmPlayerStyleRegistry.AttackStyle.MIXED
                || style == SwarmPlayerStyleRegistry.AttackStyle.UNKNOWN) {
            return;
        }
        decay(now);
        counts[index(style)] += 1.0;
        sampleMass += 1.0;
        lastObservedTick = Math.max(lastObservedTick, now);
    }

    SwarmPlayerStyleRegistry.ProfileSnapshot snapshot(long now) {
        decay(now);
        return new SwarmPlayerStyleRegistry.ProfileSnapshot(
                counts[0], counts[1], counts[2], counts[3], counts[4], counts[5],
                sampleMass, lastObservedTick);
    }

    boolean isExpired(long now) {
        return now - lastObservedTick > PROFILE_TTL_TICKS;
    }

    private void decay(long now) {
        if (now <= lastDecayTick) return;
        double factor = Math.pow(0.5,
                (double) (now - lastDecayTick) / HALF_LIFE_TICKS);
        for (int i = 0; i < counts.length; i++) counts[i] *= factor;
        sampleMass *= factor;
        lastDecayTick = now;
    }

    private static int index(SwarmPlayerStyleRegistry.AttackStyle style) {
        return switch (style) {
            case MELEE -> 0;
            case PROJECTILE -> 1;
            case EXPLOSIVE -> 2;
            case FIRE -> 3;
            case MAGIC -> 4;
            case OTHER -> 5;
            case MIXED, UNKNOWN -> throw new IllegalArgumentException("Non-observation style");
        };
    }
}
