package dev.swarmmobs.bee;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalLong;

/**
 * Bounded, disposable observation cache, NOT a limit on real bee hives.
 * Keyed by native vanilla hive and a genuinely visited flower location.
 * All reported locations must be verified by a loaded-world caller.
 */
public final class BeeFlowerMemory {
    public static final int MAX_REMEMBERED_FLOWERS_PER_HIVE = 16;
    public static final int MAX_CACHED_HIVES = 1024;
    public static final long FLOWER_MEMORY_TICKS = 2400L;

    private record Observation(double value, long seenAt) {}
    private final LinkedHashMap<Long, Map<Long, Observation>> byHive =
            new LinkedHashMap<>(16, .75f, true);

    public void report(long hive, long flower, double utility, long now) {
        if (now < 0 || !Double.isFinite(utility) || utility < 0 || utility > 1)
            throw new IllegalArgumentException("invalid flower observation");
        Map<Long, Observation> patches = byHive.computeIfAbsent(
                hive, unused -> new HashMap<>());
        Observation existing = patches.get(flower);
        if (existing != null && now < existing.seenAt()) return;
        patches.put(flower, new Observation(utility, now));
        if (patches.size() > MAX_REMEMBERED_FLOWERS_PER_HIVE) {
            long oldest = patches.entrySet().stream()
                    .min(java.util.Comparator.comparingLong(
                            (Map.Entry<Long, Observation> e) -> e.getValue().seenAt()))
                    .orElseThrow().getKey();
            patches.remove(oldest);
        }
        while (byHive.size() > MAX_CACHED_HIVES) {
            byHive.remove(byHive.keySet().iterator().next());
        }
    }

    public OptionalLong suggest(long hive, long now) {
        if (now < 0) throw new IllegalArgumentException("negative tick");
        Map<Long, Observation> patches = byHive.get(hive);
        if (patches == null) return OptionalLong.empty();
        Iterator<Map.Entry<Long, Observation>> it = patches.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            if (e.getValue().seenAt() > now
                    || now - e.getValue().seenAt() >= FLOWER_MEMORY_TICKS)
                it.remove();
        }
        if (patches.isEmpty()) {
            byHive.remove(hive);
            return OptionalLong.empty();
        }
        double best = Double.NEGATIVE_INFINITY;
        long bestPos = 0;
        for (var e : patches.entrySet()) {
            Observation o = e.getValue();
            double confidence = BeeForagePolicy.confidenceAfter(
                    now - o.seenAt(), 1.0, 600L);
            double score = o.value() * confidence;
            if (score > best || (score == best && e.getKey() < bestPos)) {
                best = score;
                bestPos = e.getKey();
            }
        }
        return OptionalLong.of(bestPos);
    }

    public void forget(long hive, long flower) {
        Map<Long, Observation> patches = byHive.get(hive);
        if (patches == null) return;
        patches.remove(flower);
        if (patches.isEmpty()) byHive.remove(hive);
    }

    public int rememberedHives() { return byHive.size(); }

    public int rememberedFlowers(long hive) {
        Map<Long, Observation> observations = byHive.get(hive);
        return observations == null ? 0 : observations.size();
    }
}
