package dev.swarmmobs.bee;

import dev.swarmmobs.visual.SwarmAnimationCue;
import java.util.LinkedHashMap;
import java.util.UUID;

/** Bounded per-bee animation cooldown memory; it never participates in AI decisions. */
public final class BeeActionMemory {
    public static final int MAX_CUES = 16384;
    public static final long MEMORY_TICKS = 12000L;
    private record Key(UUID beeId, SwarmAnimationCue cue) {}
    private final LinkedHashMap<Key, Long> lastCueTicks =
            new LinkedHashMap<>(32, 0.75f, true);

    public boolean claim(UUID beeId, SwarmAnimationCue cue, long now, long cooldownTicks) {
        if (beeId == null || cue == null || now < 0 || cooldownTicks < 0)
            throw new IllegalArgumentException("invalid bee action cue claim");
        prune(now);
        Key key = new Key(beeId, cue);
        Long previous = lastCueTicks.get(key);
        if (previous != null && now >= previous && now - previous < cooldownTicks) return false;
        lastCueTicks.put(key, now);
        while (lastCueTicks.size() > MAX_CUES)
            lastCueTicks.remove(lastCueTicks.keySet().iterator().next());
        return true;
    }

    public int size(long now) {
        if (now >= 0) prune(now);
        return lastCueTicks.size();
    }

    private void prune(long now) {
        lastCueTicks.entrySet().removeIf(entry ->
                now < entry.getValue() || now - entry.getValue() >= MEMORY_TICKS);
    }
}