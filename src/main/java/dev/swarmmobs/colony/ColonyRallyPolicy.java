package dev.swarmmobs.colony;

import java.util.Objects;
import java.util.UUID;

/** A modest, local defensive muster; never a source of enemy target locations. */
public final class ColonyRallyPolicy {
    private ColonyRallyPolicy() {}

    public static final double MIN_DISTANCE_SQUARED = 7.0 * 7.0;
    public static final double MAX_DISTANCE_SQUARED = 40.0 * 40.0;

    public static boolean mayRally(UUID member, boolean hasAlarm, boolean idle,
                                   boolean witnessChunkLoaded, double distanceSquared) {
        Objects.requireNonNull(member);
        if (!Double.isFinite(distanceSquared)) return false;
        // A fraction of the available workforce remains near home and the
        // muster never overrides a real combat target or existing search plan.
        boolean selected = Math.floorMod(member.hashCode(), 3) != 0;
        return selected && hasAlarm && idle && witnessChunkLoaded
                && distanceSquared >= MIN_DISTANCE_SQUARED
                && distanceSquared <= MAX_DISTANCE_SQUARED;
    }
}
