package dev.swarmmobs.ai;

/**
 * Interpretable adaptation layer. Player observations may influence bounded
 * formation geometry only; all low-level navigation and vanilla combat
 * handoffs remain deterministic.
 */
public final class SwarmPlayerStylePolicy {
    public record Decision(
            SwarmPlayerStyleRegistry.AttackStyle style,
            double formationRadiusMultiplier,
            double separationMultiplier,
            double cohesionMultiplier
    ) {
        public static Decision baseline() {
            return new Decision(SwarmPlayerStyleRegistry.AttackStyle.UNKNOWN,
                    1.0, 1.0, 1.0);
        }
    }

    public static Decision choose(
            SwarmPlayerStyleRegistry.ProfileSnapshot profile,
            boolean enabled
    ) {
        if (!enabled || profile == null) return Decision.baseline();
        return switch (profile.dominantStyle()) {
            case MELEE -> new Decision(
                    SwarmPlayerStyleRegistry.AttackStyle.MELEE,
                    0.92, 0.94, 1.08);
            case PROJECTILE -> new Decision(
                    SwarmPlayerStyleRegistry.AttackStyle.PROJECTILE,
                    1.18, 1.16, 0.92);
            case EXPLOSIVE -> new Decision(
                    SwarmPlayerStyleRegistry.AttackStyle.EXPLOSIVE,
                    1.24, 1.22, 0.88);
            case FIRE -> new Decision(
                    SwarmPlayerStyleRegistry.AttackStyle.FIRE,
                    1.10, 1.08, 0.96);
            case MAGIC -> new Decision(
                    SwarmPlayerStyleRegistry.AttackStyle.MAGIC,
                    1.10, 1.08, 0.96);
            case OTHER -> new Decision(
                    SwarmPlayerStyleRegistry.AttackStyle.OTHER,
                    1.10, 1.08, 0.96);
            case MIXED, UNKNOWN -> Decision.baseline();
        };
    }

    private SwarmPlayerStylePolicy() {}
}
