package dev.swarmmobs.bee;

import java.util.UUID;

/** Non-combat response to a locally observed hive-area hazard. */
public final class BeeShelterPolicy {
    public enum Mode { NORMAL, RETURN_TO_HIVE }

    public static Mode choose(BeeHiveAlarmMemory.Alert alert, UUID beeId,
            boolean hasHive, boolean hasNectar, boolean hasStung,
            boolean alreadyHasTarget, double distanceToHive) {
        if (alert == null || beeId == null || !hasHive || hasNectar || hasStung
                || alreadyHasTarget || !Double.isFinite(distanceToHive)
                || distanceToHive < 0 || distanceToHive > 20.0) {
            return Mode.NORMAL;
        }
        int shelterPercent = switch (alert.severity()) {
            case 1 -> 35;
            case 2 -> 70;
            default -> 100;
        };
        int stablePercent = Math.floorMod(beeId.hashCode(), 100);
        return stablePercent < shelterPercent
                ? Mode.RETURN_TO_HIVE : Mode.NORMAL;
    }

    private BeeShelterPolicy() {}
}
