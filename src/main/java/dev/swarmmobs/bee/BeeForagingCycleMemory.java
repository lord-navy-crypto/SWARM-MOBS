package dev.swarmmobs.bee;

import java.util.LinkedHashMap;
import java.util.UUID;

/**
 * Runtime-only lifecycle memory for vanilla Bee foraging trips.
 *
 * A completed trip is counted only when a Bee that was carrying nectar is
 * next observed empty near the same loaded hive. A rest request is cleared
 * only after the Bee has disappeared into and later re-emerged from that hive.
 * No brood or pollen state is fabricated from vanilla hive occupancy.
 */
public final class BeeForagingCycleMemory {
    public static final int MAX_BEES = 4096;
    public static final long MEMORY_TICKS = 12000L;
    public static final long MIN_REST_TICKS = 160L;
    public static final long NO_FLOWER = Long.MIN_VALUE;

    public record Snapshot(double fatigue, int completedTripsSinceRest,
            boolean completedTrip, long completedFlower, long completedHive) {}

    private record Worker(boolean hasNectar, long flower, long hive,
            long lastSeenTick, int completedTripsSinceRest,
            double fatigue, long restRequestedAt) {}

    private final LinkedHashMap<UUID, Worker> workers =
            new LinkedHashMap<>(16, 0.75f, true);

    public Snapshot observe(UUID beeId, boolean hasNectar, Long savedFlower,
            long hive, boolean nearHive, long now) {
        if (beeId == null || now < 0) {
            throw new IllegalArgumentException("invalid bee observation");
        }
        prune(now);
        Worker old = workers.get(beeId);
        if (old == null) {
            long flower = hasNectar && savedFlower != null ? savedFlower : NO_FLOWER;
            workers.put(beeId, new Worker(hasNectar, flower, hive, now, 0, 0.0,
                    Long.MIN_VALUE));
            trim();
            return snapshot(0.0, 0, false, NO_FLOWER, hive);
        }
        if (now < old.lastSeenTick()) {
            return snapshot(old.fatigue(), old.completedTripsSinceRest(),
                    false, NO_FLOWER, old.hive());
        }

        double fatigue = old.fatigue();
        int trips = old.completedTripsSinceRest();
        long restRequestedAt = old.restRequestedAt();

        // A requested rest is considered complete only after a real absence
        // from entity ticking followed by a near-hive reappearance.
        if (restRequestedAt != Long.MIN_VALUE && nearHive
                && now >= restRequestedAt
                && now - restRequestedAt >= MIN_REST_TICKS) {
            fatigue = Math.max(0.0, fatigue - 0.75);
            trips = 0;
            restRequestedAt = Long.MIN_VALUE;
        }

        boolean completed = old.hasNectar() && !hasNectar
                && nearHive && old.hive() == hive;
        long completedFlower = NO_FLOWER;
        long completedHive = hive;
        if (completed) {
            completedFlower = old.flower();
            completedHive = old.hive();
            trips = Math.min(1000, trips + 1);
            fatigue = Math.min(1.0, fatigue + 0.22);
            // Ordinary vanilla hive time provides a small recovery between
            // trips; an explicit REST assignment provides the larger recovery.
            if (now - old.lastSeenTick() >= MIN_REST_TICKS
                    && restRequestedAt == Long.MIN_VALUE) {
                fatigue = Math.max(0.0, fatigue - 0.08);
            }
        }

        long flower = hasNectar
                ? (savedFlower == null ? old.flower() : savedFlower)
                : NO_FLOWER;
        workers.put(beeId, new Worker(hasNectar, flower, hive, now,
                trips, fatigue, restRequestedAt));
        trim();
        return snapshot(fatigue, trips, completed, completedFlower, completedHive);
    }

    /** Starts a rest lease without repeatedly extending it on each tick. */
    public boolean markRestRequested(UUID beeId, long now) {
        if (beeId == null || now < 0) {
            throw new IllegalArgumentException("invalid rest request");
        }
        prune(now);
        Worker old = workers.get(beeId);
        if (old == null) {
            workers.put(beeId, new Worker(false, NO_FLOWER, 0L, now,
                    0, 0.0, now));
            trim();
            return true;
        }
        if (old.restRequestedAt() != Long.MIN_VALUE) return false;
        workers.put(beeId, new Worker(old.hasNectar(), old.flower(), old.hive(),
                now, old.completedTripsSinceRest(), old.fatigue(), now));
        return true;
    }

    public double fatigue(UUID beeId, long now) {
        if (beeId == null || now < 0) return 0.0;
        prune(now);
        Worker worker = workers.get(beeId);
        return worker == null ? 0.0 : worker.fatigue();
    }

    public int completedTrips(UUID beeId, long now) {
        if (beeId == null || now < 0) return 0;
        prune(now);
        Worker worker = workers.get(beeId);
        return worker == null ? 0 : worker.completedTripsSinceRest();
    }

    public int size(long now) {
        if (now >= 0) prune(now);
        return workers.size();
    }

    private static Snapshot snapshot(double fatigue, int trips,
            boolean completed, long flower, long hive) {
        return new Snapshot(fatigue, trips, completed, flower, hive);
    }

    private void prune(long now) {
        workers.entrySet().removeIf(entry ->
                now < entry.getValue().lastSeenTick()
                        || now - entry.getValue().lastSeenTick() > MEMORY_TICKS);
    }

    private void trim() {
        while (workers.size() > MAX_BEES) {
            workers.remove(workers.keySet().iterator().next());
        }
    }
}
