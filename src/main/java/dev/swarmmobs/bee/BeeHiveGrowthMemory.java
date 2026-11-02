package dev.swarmmobs.bee;

import java.util.LinkedHashMap;

/** Prevents several Bees from growing the same hive more than once per game tick. */
public final class BeeHiveGrowthMemory {
    public static final int MAX_TRACKED_HIVES = 1024;
    public static final long MEMORY_TICKS = 12000L;
    private final LinkedHashMap<Long, Long> lastAttemptByHive =
            new LinkedHashMap<>(16, 0.75f, true);

    public boolean claim(long hivePosition, long now) {
        if (now < 0) throw new IllegalArgumentException("negative tick");
        prune(now);
        Long previous = lastAttemptByHive.get(hivePosition);
        if (previous != null && now <= previous) return false;
        lastAttemptByHive.put(hivePosition, now);
        while (lastAttemptByHive.size() > MAX_TRACKED_HIVES) {
            lastAttemptByHive.remove(lastAttemptByHive.keySet().iterator().next());
        }
        return true;
    }

    public int size(long now) {
        if (now >= 0) prune(now);
        return lastAttemptByHive.size();
    }

    private void prune(long now) {
        lastAttemptByHive.entrySet().removeIf(entry ->
                now >= entry.getValue() && now - entry.getValue() >= MEMORY_TICKS);
    }
}
