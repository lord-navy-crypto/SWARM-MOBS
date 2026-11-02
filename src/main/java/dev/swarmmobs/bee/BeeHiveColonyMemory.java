package dev.swarmmobs.bee;

import java.util.LinkedHashMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;

/** Bounded transient cache and overflow-slot reservations for vanilla hive colonies. */
public final class BeeHiveColonyMemory {
    public static final int MAX_CACHED_HIVES = 1024;
    public static final int MAX_RESERVATIONS = 4096;
    public static final long SNAPSHOT_TTL_TICKS = 20L;
    public static final long RESERVATION_TTL_TICKS = 600L;

    private record Cached(BeeHiveColonySnapshot.Snapshot snapshot, long observedAt, int maxHives) {}
    private record Reservation(long targetHive, long expiresAt) {}

    private final LinkedHashMap<Long, Cached> snapshots = new LinkedHashMap<>(16, 0.75f, true);
    private final LinkedHashMap<UUID, Reservation> reservations = new LinkedHashMap<>(16, 0.75f, true);

    public BeeHiveColonySnapshot.Snapshot snapshot(ServerLevel level, BlockPos home,
            long now, int maxHives) {
        if (level == null || home == null || now < 0)
            throw new IllegalArgumentException("invalid hive snapshot request");
        pruneReservations(now);
        Cached cached = snapshots.get(home.asLong());
        if (cached != null && cached.maxHives() == maxHives && now >= cached.observedAt()
                && now - cached.observedAt() < SNAPSHOT_TTL_TICKS) return cached.snapshot();

        BeeHiveColonySnapshot.Snapshot fresh = BeeHiveColonySnapshot.scan(level, home, maxHives);
        Cached entry = new Cached(fresh, now, maxHives);
        for (BlockPos position : fresh.positions()) snapshots.put(position.asLong(), entry);
        trimSnapshots();
        return fresh;
    }

    public void invalidate(BeeHiveColonySnapshot.Snapshot snapshot) {
        if (snapshot != null)
            for (BlockPos position : snapshot.positions()) snapshots.remove(position.asLong());
    }

    public boolean reserve(UUID beeId, BlockPos targetHive, int storedOccupants, long now) {
        if (beeId == null || targetHive == null || storedOccupants < 0 || now < 0)
            throw new IllegalArgumentException("invalid hive reservation");
        pruneReservations(now);
        Reservation old = reservations.get(beeId);
        if (old != null && old.targetHive() == targetHive.asLong()) return true;
        reservations.remove(beeId);
        if (storedOccupants + reservedSlots(targetHive.asLong(), now)
                >= BeehiveBlockEntity.MAX_OCCUPANTS) return false;
        reservations.put(beeId, new Reservation(targetHive.asLong(),
                Math.addExact(now, RESERVATION_TTL_TICKS)));
        while (reservations.size() > MAX_RESERVATIONS)
            reservations.remove(reservations.keySet().iterator().next());
        return true;
    }

    public int reservedSlots(long hivePosition, long now) {
        pruneReservations(now);
        int count = 0;
        for (Reservation reservation : reservations.values())
            if (reservation.targetHive() == hivePosition) count++;
        return count;
    }

    public boolean hasReservation(UUID beeId, long now) {
        pruneReservations(now);
        return beeId != null && reservations.containsKey(beeId);
    }

    public void reconcile(UUID beeId, BlockPos currentHive, long now) {
        if (beeId == null) return;
        pruneReservations(now);
        Reservation reservation = reservations.get(beeId);
        if (reservation != null && (currentHive == null
                || reservation.targetHive() != currentHive.asLong())) reservations.remove(beeId);
    }

    public void release(UUID beeId) {
        if (beeId != null) reservations.remove(beeId);
    }

    public int cachedHiveCount() { return snapshots.size(); }
    public int reservationCount(long now) {
        pruneReservations(now);
        return reservations.size();
    }

    private void pruneReservations(long now) {
        reservations.entrySet().removeIf(entry -> now >= entry.getValue().expiresAt());
    }

    private void trimSnapshots() {
        while (snapshots.size() > MAX_CACHED_HIVES)
            snapshots.remove(snapshots.keySet().iterator().next());
    }
}
