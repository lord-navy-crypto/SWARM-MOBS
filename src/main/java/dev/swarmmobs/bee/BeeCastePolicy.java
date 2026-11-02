package dev.swarmmobs.bee;

import java.util.UUID;

/** Stable behavioral castes for vanilla Bee individuals; visuals and entity type remain vanilla. */
public final class BeeCastePolicy {
    public enum Caste { FORAGER, NURSE, GUARD, BUILDER, SCOUT }
    private BeeCastePolicy() {}

    public static Caste caste(UUID beeId) {
        if (beeId == null) throw new IllegalArgumentException("beeId");
        long mixed = beeId.getMostSignificantBits()
                ^ Long.rotateLeft(beeId.getLeastSignificantBits(), 23);
        int bucket = (int) Long.remainderUnsigned(mixed, 100L);
        if (bucket < 42) return Caste.FORAGER;
        if (bucket < 60) return Caste.NURSE;
        if (bucket < 74) return Caste.GUARD;
        if (bucket < 88) return Caste.BUILDER;
        return Caste.SCOUT;
    }

    public static BeeTaskPolicy.Worker workerProfile(UUID beeId) {
        return switch (caste(beeId)) {
            case FORAGER -> new BeeTaskPolicy.Worker(0.98, 0.20, 0.15, 0.20);
            case NURSE -> new BeeTaskPolicy.Worker(0.35, 0.98, 0.20, 0.15);
            case GUARD -> new BeeTaskPolicy.Worker(0.20, 0.20, 0.98, 0.15);
            case BUILDER -> new BeeTaskPolicy.Worker(0.20, 0.15, 0.20, 0.98);
            case SCOUT -> new BeeTaskPolicy.Worker(0.85, 0.35, 0.60, 0.25);
        };
    }
}
