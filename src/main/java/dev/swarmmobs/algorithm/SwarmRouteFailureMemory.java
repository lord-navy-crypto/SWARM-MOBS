package dev.swarmmobs.algorithm;

import net.minecraft.core.BlockPos;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bounded, short-lived stigmergic memory of locally rejected navigation cells.
 * It remembers only a position already tested by PathNavigation; it never
 * discovers or scans new terrain and never forces a chunk to load.
 */
public final class SwarmRouteFailureMemory {
    public static final int MAX_ENTRIES = 2048;
    public static final long FAILURE_WINDOW_TICKS = 80L;

    private record Failure(int count, long lastFailureTick) {}
    private final LinkedHashMap<Long, Failure> failures =
            new LinkedHashMap<>(16, 0.75f, true);

    public void reportFailure(BlockPos position, long now) {
        if (position == null || now < 0) {
            throw new IllegalArgumentException("invalid route failure");
        }
        prune(now);
        long key = position.asLong();
        Failure previous = failures.get(key);
        if (previous != null && now < previous.lastFailureTick()) return;
        if (previous != null && now == previous.lastFailureTick()) return;
        int count = previous != null
                && now - previous.lastFailureTick() <= FAILURE_WINDOW_TICKS
                ? Math.min(8, previous.count() + 1) : 1;
        failures.put(key, new Failure(count, now));
        trim();
    }

    public boolean shouldAvoid(BlockPos position, long now,
            int threshold, long memoryTicks) {
        if (position == null || now < 0 || threshold < 1 || memoryTicks < 1) {
            return false;
        }
        prune(now);
        Failure failure = failures.get(position.asLong());
        if (failure == null || now < failure.lastFailureTick()) return false;
        return failure.count() >= threshold
                && now - failure.lastFailureTick() < memoryTicks;
    }

    /** A successful local path query or accepted movement clears old failure evidence. */
    public void reportSuccess(BlockPos position) {
        if (position != null) failures.remove(position.asLong());
    }

    public int size(long now) {
        if (now >= 0) prune(now);
        return failures.size();
    }

    public int failureCount(BlockPos position, long now) {
        if (position == null || now < 0) return 0;
        prune(now);
        Failure failure = failures.get(position.asLong());
        return failure == null ? 0 : failure.count();
    }

    private void prune(long now) {
        failures.entrySet().removeIf(entry ->
                now < entry.getValue().lastFailureTick()
                        || now - entry.getValue().lastFailureTick() > FAILURE_WINDOW_TICKS * 3);
    }

    private void trim() {
        while (failures.size() > MAX_ENTRIES) {
            failures.remove(failures.keySet().iterator().next());
        }
    }
}
