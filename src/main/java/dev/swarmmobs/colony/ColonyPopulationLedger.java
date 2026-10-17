package dev.swarmmobs.colony;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Counts unique, recently ticking members by their persistent root.
 * Does not load chunks, hold Entity references, or assume a maximum colony size.
 * A stale observation expires after a caller-selected number of game ticks.
 */
public final class ColonyPopulationLedger {
    private final Map<Long, Map<UUID, Long>> byRoot = new HashMap<>();

    public void observe(long root, UUID memberId, long tick) {
        Objects.requireNonNull(memberId);
        if (tick < 0) throw new IllegalArgumentException("negative game tick");
        byRoot.computeIfAbsent(root, ignored -> new HashMap<>())
                .merge(memberId, tick, Math::max);
    }

    /** Removes stale members of this root; visits only its own recorded members. */
    public int count(long root, long now, long expiryTicks) {
        if (now < 0 || expiryTicks < 1)
            throw new IllegalArgumentException("invalid observation age");
        Map<UUID, Long> members = byRoot.get(root);
        if (members == null) return 0;
        Iterator<Map.Entry<UUID, Long>> it = members.entrySet().iterator();
        while (it.hasNext()) {
            long seenAt = it.next().getValue();
            if (seenAt > now || now - seenAt >= expiryTicks) it.remove();
        }
        int result = members.size();
        if (result == 0) byRoot.remove(root);
        return result;
    }

    public int trackedRoots() { return byRoot.size(); }
}
