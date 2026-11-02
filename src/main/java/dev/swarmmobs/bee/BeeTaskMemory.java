package dev.swarmmobs.bee;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Bounded, runtime-only task hysteresis for individual vanilla Bees.
 * This selects colony priorities; it does not replace vanilla Bee goals.
 */
public final class BeeTaskMemory {
    public static final int MAX_ASSIGNMENTS = 4096;
    public static final long MEMORY_TICKS = 12000L;
    private static final double SWITCH_PENALTY = 0.14;

    private record Assignment(BeeTaskPolicy.Task task, long updatedAt) {}
    private final LinkedHashMap<UUID, Assignment> assignments =
            new LinkedHashMap<>(16, 0.75f, true);

    public BeeTaskPolicy.Task assign(UUID beeId, BeeTaskPolicy.Needs needs, long now) {
        if (beeId == null || needs == null || now < 0)
            throw new IllegalArgumentException("invalid bee task assignment");
        prune(now);
        Assignment old = assignments.get(beeId);
        if (old != null && now < old.updatedAt()) return old.task();
        BeeTaskPolicy.Task previous = old == null
                ? BeeTaskPolicy.Task.FORAGE : old.task();
        BeeTaskPolicy.Task selected = BeeTaskPolicy.select(
                needs, workerProfile(beeId), previous, SWITCH_PENALTY);
        assignments.put(beeId, new Assignment(selected, now));
        while (assignments.size() > MAX_ASSIGNMENTS) {
            assignments.remove(assignments.keySet().iterator().next());
        }
        return selected;
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
        int hash = id.hashCode();
        double forage = unit(hash);
        double nurse = unit(Integer.rotateLeft(hash, 7));
        double guard = unit(Integer.rotateLeft(hash, 13));
        double build = unit(Integer.rotateLeft(hash, 21));
        return new BeeTaskPolicy.Worker(
                0.55 + 0.45 * forage,
                0.20 + 0.55 * nurse,
                0.10 + 0.90 * guard,
                0.15 + 0.70 * build
        );
    }

    private static double unit(int value) {
        return (Integer.toUnsignedLong(value) & 0xFFFFL) / 65535.0;
    }
}
