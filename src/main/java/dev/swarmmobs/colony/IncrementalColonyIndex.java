package dev.swarmmobs.colony;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * An incremental, face-connected colony scan without a lifetime size cap.
 * Work is bounded PER STEP, never by total colony size.
 *
 * One instance belongs to one kind of colony (monster or bee) and one origin.
 * World access is prohibited in unloaded chunks by the caller predicate.
 * Pending inaccessible cells pause until the region is loaded again.
 * Call reset on topology change if an accurate split is needed.
 */
public final class IncrementalColonyIndex {
    private final ArrayDeque<ConnectedColonyLayout.Cell> pending = new ArrayDeque<>();
    private final Set<ConnectedColonyLayout.Cell> inspected = new HashSet<>();
    private final Set<ConnectedColonyLayout.Cell> members = new HashSet<>();
    private final Set<ConnectedColonyLayout.Cell> deferred = new HashSet<>();
    private boolean initialized;

    public IncrementalColonyIndex(ConnectedColonyLayout.Cell origin) {
        pending.add(Objects.requireNonNull(origin));
    }

    /**
     * Processes at most budget candidate cells. The loaded predicate MUST check
     * the chunk before any block-state reads. Deferred cells can be retried later.
     */
    public int step(int budget, Predicate<ConnectedColonyLayout.Cell> loaded,
                    Predicate<ConnectedColonyLayout.Cell> sameBlock) {
        Objects.requireNonNull(loaded);
        Objects.requireNonNull(sameBlock);
        if (budget < 1) throw new IllegalArgumentException("budget");
        initialized = true;
        int processed = 0;
        while (processed < budget && !pending.isEmpty()) {
            var cell = pending.removeFirst();
            if (inspected.contains(cell) || deferred.contains(cell)) continue;
            processed++;
            if (!loaded.test(cell)) {
                deferred.add(cell);
                continue;
            }
            inspected.add(cell);
            if (!sameBlock.test(cell)) continue;
            if (members.add(cell)) {
                for (var neighbor : cell.neighbors()) {
                    if (!inspected.contains(neighbor) && !deferred.contains(neighbor))
                        pending.addLast(neighbor);
                }
            }
        }
        return processed;
    }

    /** Retry frontier cells previously unavailable because their chunk was unloaded. */
    public void retryDeferred() {
        pending.addAll(deferred);
        deferred.clear();
    }

    public int knownBlocks() { return members.size(); }
    public boolean completeForLoadedRegion() {
        return initialized && pending.isEmpty();
    }
    public boolean hasDeferredFrontier() { return !deferred.isEmpty(); }

    /** Saturates integer capacity but does not impose a colony size ceiling. */
    public long capacity(long capacityPerBlock) {
        if (capacityPerBlock < 1) throw new IllegalArgumentException("capacityPerBlock");
        return Math.multiplyExact((long) members.size(), capacityPerBlock);
    }

    public Set<ConnectedColonyLayout.Cell> knownCells() {
        return Set.copyOf(members);
    }

    /** Re-index after a block is removed, allowing disconnected groups to split. */
    public void reset(ConnectedColonyLayout.Cell origin) {
        pending.clear();
        inspected.clear();
        members.clear();
        deferred.clear();
        initialized = false;
        pending.add(Objects.requireNonNull(origin));
    }
}
