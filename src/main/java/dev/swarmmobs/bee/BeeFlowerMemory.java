package dev.swarmmobs.bee;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

/**
 * Bounded, disposable observation cache, NOT a limit on real bee hives.
 * Keyed by native vanilla hive and genuinely visited flower locations.
 * All reported locations must be verified by a loaded-world caller.
 */
public final class BeeFlowerMemory {
    public static final int MAX_REMEMBERED_FLOWERS_PER_HIVE = 16;
    public static final int MAX_CACHED_HIVES = 1024;
    public static final long FLOWER_MEMORY_TICKS = 2400L;

    public record FlowerObservation(long flower, double utility, long seenAt,
            int successfulReturns, double confidence) {}

    private record Observation(double value, long seenAt, int successfulReturns) {}
    private final LinkedHashMap<Long, Map<Long, Observation>> byHive =
            new LinkedHashMap<>(16, .75f, true);

    public void report(long hive, long flower, double utility, long now) {
        if (now < 0 || !Double.isFinite(utility) || utility < 0 || utility > 1)
            throw new IllegalArgumentException("invalid flower observation");
        Map<Long, Observation> patches = byHive.computeIfAbsent(
                hive, unused -> new HashMap<>());
        Observation existing = patches.get(flower);
        if (existing != null && now < existing.seenAt()) return;
        int returns = existing == null ? 1
                : Math.min(16, existing.successfulReturns() + 1);
        double smoothed = existing == null ? utility
                : 0.65 * existing.value() + 0.35 * utility;
        patches.put(flower, new Observation(smoothed, now, returns));
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

    /** Fresh observations for caller-side distance, competition and danger scoring. */
    public List<FlowerObservation> observations(long hive, long now) {
        if (now < 0) throw new IllegalArgumentException("negative tick");
        Map<Long, Observation> patches = byHive.get(hive);
        if (patches == null) return List.of();
        Iterator<Map.Entry<Long, Observation>> it = patches.entrySet().iterator();
        List<FlowerObservation> result = new ArrayList<>();
        while (it.hasNext()) {
            var entry = it.next();
            Observation o = entry.getValue();
            if (o.seenAt() > now || now - o.seenAt() >= FLOWER_MEMORY_TICKS) {
                it.remove();
                continue;
            }
            double freshness = BeeForagePolicy.confidenceAfter(
                    now - o.seenAt(), 1.0, 600L);
            double reliability = Math.min(1.0, 0.55 + 0.09 * o.successfulReturns());
            result.add(new FlowerObservation(entry.getKey(), o.value(), o.seenAt(),
                    o.successfulReturns(), freshness * reliability));
        }
        if (patches.isEmpty()) byHive.remove(hive);
        return List.copyOf(result);
    }

    public OptionalLong suggest(long hive, long now) {
        double best = Double.NEGATIVE_INFINITY;
        long bestPos = 0;
        boolean found = false;
        for (FlowerObservation observation : observations(hive, now)) {
            double score = observation.utility() * observation.confidence();
            if (!found || score > best
                    || (score == best && observation.flower() < bestPos)) {
                found = true;
                best = score;
                bestPos = observation.flower();
            }
        }
        return found ? OptionalLong.of(bestPos) : OptionalLong.empty();
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
