package dev.swarmmobs.bee;

import java.util.Collection;
import java.util.LinkedHashMap;

/** Deduplicates connected-colony evaluations and rate-limits successful hive growth. */
public final class BeeHiveGrowthMemory {
    public static final int MAX_TRACKED_HIVES = 1024;
    public static final long MEMORY_TICKS = 12000L;
    private final LinkedHashMap<Long, Long> lastEvaluationByHive = new LinkedHashMap<>(16, 0.75f, true);
    private final LinkedHashMap<Long, Long> lastExpansionByHive = new LinkedHashMap<>(16, 0.75f, true);

    public boolean claimColony(Collection<Long> hives, long now, long cooldownTicks) {
        if (hives == null || hives.isEmpty() || now < 0 || cooldownTicks < 0)
            throw new IllegalArgumentException("invalid colony growth claim");
        prune(now);
        for (Long hive : hives) {
            if (hive == null) throw new IllegalArgumentException("null hive position");
            long key = hive;
            Long evaluated = lastEvaluationByHive.get(key);
            if (evaluated != null && evaluated == now) return false;
            Long expanded = lastExpansionByHive.get(key);
            if (expanded != null && (now < expanded || now - expanded < cooldownTicks)) return false;
        }
        for (Long hive : hives) lastEvaluationByHive.put(hive, now);
        trim(lastEvaluationByHive);
        return true;
    }

    public void recordExpansion(Collection<Long> hives, long now) {
        if (hives == null || hives.isEmpty() || now < 0)
            throw new IllegalArgumentException("invalid expansion record");
        prune(now);
        for (Long hive : hives) {
            if (hive == null) throw new IllegalArgumentException("null hive position");
            lastExpansionByHive.put(hive, now);
            lastEvaluationByHive.put(hive, now);
        }
        trim(lastExpansionByHive);
        trim(lastEvaluationByHive);
    }

    public boolean claim(long hivePosition, long now) {
        if (now < 0) throw new IllegalArgumentException("negative tick");
        prune(now);
        Long previous = lastEvaluationByHive.get(hivePosition);
        if (previous != null && now <= previous) return false;
        lastEvaluationByHive.put(hivePosition, now);
        trim(lastEvaluationByHive);
        return true;
    }

    public int size(long now) {
        if (now >= 0) prune(now);
        return lastEvaluationByHive.size();
    }

    private void prune(long now) {
        lastEvaluationByHive.entrySet().removeIf(e -> now >= e.getValue()
                && now - e.getValue() >= MEMORY_TICKS);
        lastExpansionByHive.entrySet().removeIf(e -> now >= e.getValue()
                && now - e.getValue() >= MEMORY_TICKS);
    }

    private static void trim(LinkedHashMap<Long, Long> map) {
        while (map.size() > MAX_TRACKED_HIVES) map.remove(map.keySet().iterator().next());
    }
}
