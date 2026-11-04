package dev.swarmmobs.algorithm;

/**
 * Decides when a Spider should trust Minecraft's native wall-climber
 * navigation instead of being diverted into a generic horizontal detour.
 * Terrain measurements are supplied by the caller so this policy is pure.
 */
public final class SwarmSpiderTerrainPolicy {
    public static final int MIN_CLIMB_WALL_HEIGHT = 2;
    public static final int MAX_CLIMB_WALL_HEIGHT = 5;

    private SwarmSpiderTerrainPolicy() {}

    public static boolean preferNativeClimb(
            boolean spider,
            boolean wallClimberNavigation,
            boolean frontBlocked,
            int contiguousWallHeight,
            boolean landingHeadroomClear,
            double targetDeltaY
    ) {
        if (!spider || !wallClimberNavigation || !frontBlocked
                || !landingHeadroomClear || !Double.isFinite(targetDeltaY)
                || targetDeltaY < -0.5) {
            return false;
        }
        return contiguousWallHeight >= MIN_CLIMB_WALL_HEIGHT
                && contiguousWallHeight <= MAX_CLIMB_WALL_HEIGHT;
    }
}
