package dev.swarmmobs.ai;

/** Decaying, bounded evidence for a player's observed movement style. */
final class SwarmPlayerMotionProfile {
    static final long HALF_LIFE_TICKS = 2400L;
    static final long PROFILE_TTL_TICKS = 12000L;
    private final double[] counts = new double[4];
    private double sampleMass;
    private long lastDecayTick;
    private long lastObservedTick;
    private long lastSampleTick = Long.MIN_VALUE;
    private double previousDirectionX;
    private double previousDirectionZ;
    private boolean hasPreviousDirection;

    SwarmPlayerMotionProfile(long now) {
        lastDecayTick = now;
        lastObservedTick = now;
    }

    void observe(double velocityX, double velocityZ, long observationTick, long now) {
        if (!Double.isFinite(velocityX) || !Double.isFinite(velocityZ)
                || observationTick < 0 || now < 0 || observationTick <= lastSampleTick) return;
        decay(now);
        double speed = Math.hypot(velocityX, velocityZ);
        SwarmPlayerMotionRegistry.MotionStyle style;
        if (speed < 0.035) {
            style = SwarmPlayerMotionRegistry.MotionStyle.STATIONARY;
        } else if (speed < 0.18) {
            style = SwarmPlayerMotionRegistry.MotionStyle.WALKING;
        } else {
            style = SwarmPlayerMotionRegistry.MotionStyle.SPRINTING;
        }

        if (speed >= 0.08 && hasPreviousDirection
                && observationTick - lastSampleTick <= 40) {
            double directionX = velocityX / speed;
            double directionZ = velocityZ / speed;
            double alignment = directionX * previousDirectionX
                    + directionZ * previousDirectionZ;
            if (alignment < 0.25) {
                style = SwarmPlayerMotionRegistry.MotionStyle.ERRATIC;
            }
        }

        counts[index(style)] += 1.0;
        sampleMass += 1.0;
        lastObservedTick = Math.max(lastObservedTick, now);
        lastSampleTick = observationTick;
        if (speed >= 0.08) {
            previousDirectionX = velocityX / speed;
            previousDirectionZ = velocityZ / speed;
            hasPreviousDirection = true;
        }
    }

    SwarmPlayerMotionRegistry.ProfileSnapshot snapshot(long now) {
        decay(now);
        return new SwarmPlayerMotionRegistry.ProfileSnapshot(
                counts[0], counts[1], counts[2], counts[3], sampleMass, lastObservedTick);
    }

    boolean isExpired(long now) {
        return now >= lastObservedTick && now - lastObservedTick > PROFILE_TTL_TICKS;
    }

    private void decay(long now) {
        if (now <= lastDecayTick) return;
        double factor = Math.pow(0.5, (double) (now - lastDecayTick) / HALF_LIFE_TICKS);
        for (int i = 0; i < counts.length; i++) counts[i] *= factor;
        sampleMass *= factor;
        lastDecayTick = now;
    }

    private static int index(SwarmPlayerMotionRegistry.MotionStyle style) {
        return switch (style) {
            case STATIONARY -> 0;
            case WALKING -> 1;
            case SPRINTING -> 2;
            case ERRATIC -> 3;
            case MIXED, UNKNOWN -> throw new IllegalArgumentException("Non-observation motion style");
        };
    }
}
