package dev.swarmmobs.ai;

/** Converts confident, decayed movement evidence into conservative formation adjustments. */
public final class SwarmPlayerMotionPolicy {
    public record Decision(SwarmPlayerMotionRegistry.MotionStyle style,
            double formationRadiusMultiplier, double separationMultiplier,
            double cohesionMultiplier, double searchRadiusMultiplier) {
        public static Decision baseline() {
            return new Decision(SwarmPlayerMotionRegistry.MotionStyle.UNKNOWN,
                    1.0, 1.0, 1.0, 1.0);
        }
    }

    public static Decision choose(SwarmPlayerMotionRegistry.ProfileSnapshot profile,
            boolean enabled) {
        if (!enabled || profile == null) return Decision.baseline();
        return switch (profile.dominantStyle()) {
            case STATIONARY -> new Decision(
                    SwarmPlayerMotionRegistry.MotionStyle.STATIONARY, 0.94, 1.0, 1.06, 0.90);
            case WALKING -> new Decision(
                    SwarmPlayerMotionRegistry.MotionStyle.WALKING, 1.0, 1.0, 1.0, 1.0);
            case SPRINTING -> new Decision(
                    SwarmPlayerMotionRegistry.MotionStyle.SPRINTING, 1.10, 1.08, 0.94, 1.10);
            case ERRATIC -> new Decision(
                    SwarmPlayerMotionRegistry.MotionStyle.ERRATIC, 1.18, 1.15, 0.88, 1.18);
            case MIXED, UNKNOWN -> Decision.baseline();
        };
    }

    private SwarmPlayerMotionPolicy() {}
}
