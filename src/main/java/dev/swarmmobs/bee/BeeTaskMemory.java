package dev.swarmmobs.bee;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Bounded, runtime-only task hysteresis and hive-local workforce accounting
 * for individual vanilla Bees. This selects colony priorities; it does not
 * replace vanilla Bee goals.
 */
public final class BeeTaskMemory {
    public static final int MAX_ASSIGNMENTS = 4096;
    public static final long MEMORY_TICKS = 12000L;
    public static final long TASK_COUNT_TTL_TICKS = 400L;
    private static final double SWITCH_PENALTY = 0.14;

    private record Assignment(BeeTaskPolicy.Task task, long colonyKey, long updatedAt) {}
    private final LinkedHashMap<UUID, Assignment> assignments =
            new LinkedHashMap<>(16, 0.75f, true);

    /** Compatibility entry point for callers without a hive identity. */
    public BeeTaskPolicy.Task assign(UUID beeId, BeeTaskPolicy.Needs needs, long now) {
        return assign(beeId, 0L, needs, now);
    }

    public BeeTaskPolicy.Task assign(UUID beeId, long colonyKey,
                                     BeeTaskPolicy.Needs needs, long now) {
        if (beeId == null || needs == null || now < 0)
            throw new IllegalArgumentException("invalid bee task assignment");
        prune(now);
        Assignment old = assignments.get(beeId);
        if (old != null && now < old.updatedAt()) return old.task();

        boolean sameColony = old != null && old.colonyKey() == colonyKey;
        BeeTaskPolicy.Task previous = sameColony
                ? old.task() : BeeTaskPolicy.Task.FORAGE;
        // A full hive or completed rest can suppress fatigue to zero. Do not
        // keep a stale REST assignment alive after its need has disappeared.
        if ((previous == BeeTaskPolicy.Task.REST && needs.fatigue() == 0.0)
                || (previous == BeeTaskPolicy.Task.BUILD && needs.spaceShortage() == 0.0)
                || (previous == BeeTaskPolicy.Task.GUARD && needs.threat() == 0.0)
                || (previous == BeeTaskPolicy.Task.NURSE && needs.broodDemand() == 0.0)) {
            previous = BeeTaskPolicy.Task.FORAGE;
        }

        BeeTaskPolicy.Workforce workforce = workforce(colonyKey, now, beeId);
        BeeTaskPolicy.Task selected = BeeTaskPolicy.select(
                needs, workerProfile(beeId), previous, SWITCH_PENALTY, workforce);
        assignments.put(beeId, new Assignment(selected, colonyKey, now));
        while (assignments.size() > MAX_ASSIGNMENTS) {
            assignments.remove(assignments.keySet().iterator().next());
        }
        return selected;
    }

    /**
     * Returns a bounded estimate from assignments refreshed within the task
     * census window. No entity scan, chunk load, or cross-hive global manager.
     */
    BeeTaskPolicy.Workforce workforce(long colonyKey, long now, UUID excludeBee) {
        int forage = 0, nurse = 0, guard = 0, build = 0, rest = 0;
        for (Map.Entry<UUID, Assignment> entry : assignments.entrySet()) {
            if (entry.getKey().equals(excludeBee)) continue;
            Assignment assignment = entry.getValue();
            if (assignment.colonyKey() != colonyKey
                    || now < assignment.updatedAt()
                    || now - assignment.updatedAt() > TASK_COUNT_TTL_TICKS) {
                continue;
            }
            switch (assignment.task()) {
                case FORAGE -> forage++;
                case NURSE -> nurse++;
                case GUARD -> guard++;
                case BUILD -> build++;
                case REST -> rest++;
            }
        }
        int active = forage + nurse + guard + build + rest;
        return new BeeTaskPolicy.Workforce(
                Math.max(1, active + 1), forage, nurse, guard, build, rest);
    }

    public void clear(UUID beeId) {
        if (beeId != null) assignments.remove(beeId);
    }

    public int size(long now) {
        if (now >= 0) prune(now);
        return assignments.size();
    }

    private void prune(long now) {
        assignments.entrySet().removeIf(entry ->
                now >= entry.getValue().updatedAt()
                        && now - entry.getValue().updatedAt() >= MEMORY_TICKS);
    }

    private static BeeTaskPolicy.Worker workerProfile(UUID id) {
        return BeeCastePolicy.workerProfile(id);
    }
}